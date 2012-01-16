package org.strawberry.guice;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.strawberry.util.JedisUtil.destroyOnShutdown;

/**
 *
 * @author Wiehann Matthysen
 */
public class BigDecimalInjectionTest extends AbstractModule {

    private final JedisPool pool = destroyOnShutdown(new JedisPool("localhost", 6379));

    private Injector injector;
    private Jedis jedis;

    @Override
    protected void configure() {
        install(new RedisModule(this.pool));
    }

    @Before
    public void setup() {
        this.injector = Guice.createInjector(this);
        this.jedis = this.pool.getResource();
    }

    @After
    public void teardown() {
        for (String key : this.jedis.keys("test:*")) {
            this.jedis.del(key);
        }
        this.pool.returnResource(this.jedis);
    }



    public static class BigDecimalWithoutKey {

        @Redis("test:bigdecimal")
        private BigDecimal bigDecimal;

        public BigDecimal getInjectedBigDecimal() {
            return this.bigDecimal;
        }
    }

    public static class BigDecimalWithoutKeyAllowNull {

        @Redis(value = "test:bigdecimal", allowNull = true)
        private BigDecimal bigDecimal;

        public BigDecimal getInjectedBigDecimal() {
            return this.bigDecimal;
        }
    }

    @Test
    public void test_that_string_without_key_is_converted_into_bigdecimal() {
        this.jedis.set("test:bigdecimal", "123.456");
        BigDecimalWithoutKey dummy = this.injector.getInstance(BigDecimalWithoutKey.class);
        assertThat(dummy.getInjectedBigDecimal().doubleValue(), is(123.456));
        this.jedis.set("test:bigdecimal", ".123");
        dummy = this.injector.getInstance(BigDecimalWithoutKey.class);
        assertThat(dummy.getInjectedBigDecimal().doubleValue(), is(0.123));
    }

    @Test
    public void test_that_missing_value_is_injected_as_null_into_bigdecimal() {
        BigDecimalWithoutKeyAllowNull dummy = this.injector.getInstance(
            BigDecimalWithoutKeyAllowNull.class);
        assertThat(dummy.getInjectedBigDecimal(), is(nullValue()));
    }

    @Test
    public void test_that_missing_value_is_injected_as_zero_into_bigdecimal() {
        BigDecimalWithoutKey dummy = this.injector.getInstance(BigDecimalWithoutKey.class);
        assertThat(dummy.getInjectedBigDecimal().doubleValue(), is(0.0));
    }

    @Test(expected = RuntimeException.class)
    public void test_that_invalid_string_throws_exception_when_converting_to_bigdecimal() {
        this.jedis.set("test:bigdecimal", "invalid");
        this.injector.getInstance(BigDecimalWithoutKey.class);
    }

    @Test
    public void test_that_too_small_value_doesnt_overflow_to_infinity_when_converting_to_bigdecimal() {
        String value = "-179.76931348623159E+306";
        this.jedis.set("test:bigdecimal", value);
        BigDecimalWithoutKey dummy = this.injector.getInstance(BigDecimalWithoutKey.class);
        assertThat(dummy.getInjectedBigDecimal().toEngineeringString(), is(equalTo(value)));
    }

    @Test
    public void test_that_too_large_value_doesnt_overflow_to_infinity_when_converting_to_bigdecimal() {
        String value = "179.76931348623159E+306";
        this.jedis.set("test:bigdecimal", value);
        BigDecimalWithoutKey dummy = this.injector.getInstance(BigDecimalWithoutKey.class);
        assertThat(dummy.getInjectedBigDecimal().toEngineeringString(), is(equalTo(value)));
    }
}