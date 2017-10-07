package io.github.jonestimd.finance.file;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class DomainMapperTest {
    @Test
    public void returnsExactMatch() throws Exception {
        List<TestBean> beans = Lists.newArrayList(new TestBean("bean10"), new TestBean("bean1"), new TestBean("bean2"));
        DomainMapper<TestBean> mapper = new DomainMapper<>(beans, TestBean::getName, null);

        assertThat(mapper.get("bean1")).isSameAs(beans.get(1));
        assertThat(mapper.get("BEAN1")).isSameAs(beans.get(1));
        assertThat(mapper.get("bean2")).isSameAs(beans.get(2));
    }

    @Test
    public void returnsContainsMatch() throws Exception {
        List<TestBean> beans = Lists.newArrayList(new TestBean("bean1 with extra"), new TestBean("bean1"), new TestBean("bean2"));
        DomainMapper<TestBean> mapper = new DomainMapper<>(beans, TestBean::getName, null);

        assertThat(mapper.get("bean1 with extra info")).isSameAs(beans.get(0));
        assertThat(mapper.get("extra bean1 info")).isSameAs(beans.get(1));
    }

    @Test
    public void returnsAliasMatch() throws Exception {
        List<TestBean> beans = Lists.newArrayList(new TestBean("bean1"), new TestBean("bean2"));
        TestBean bean = new TestBean("alias1");
        Map<String, TestBean> aliases = ImmutableMap.of("bean1", bean);
        DomainMapper<TestBean> mapper = new DomainMapper<>(beans, TestBean::getName, aliases, null);

        assertThat(mapper.get("bean1")).isSameAs(bean);
        assertThat(mapper.get("BEAN1")).isNotSameAs(bean);
    }

    @Test
    public void returnsNewBeanIfNoMatch() throws Exception {
        List<TestBean> beans = Lists.newArrayList(new TestBean("bean1"), new TestBean("bean2"));
        Map<String, TestBean> aliases = ImmutableMap.of("ALIAS1", beans.get(0));
        DomainMapper<TestBean> mapper = new DomainMapper<>(beans, TestBean::getName, aliases, TestBean::new);

        final TestBean result = mapper.get("bean3");

        assertThat(result.getName()).isEqualTo("bean3");
        assertThat(mapper.get("bean3")).isSameAs(result).as("only create target once");
    }

    @Test
    public void doesNotAddNullToTargets() throws Exception {
        List<TestBean> beans = Lists.newArrayList(new TestBean("bean1"), new TestBean("bean2"));
        Map<String, TestBean> aliases = ImmutableMap.of("ALIAS1", beans.get(0));
        DomainMapper<TestBean> mapper = new DomainMapper<>(beans, TestBean::getName, aliases, null);

        assertThat(mapper.get("bean3")).isNull();

        assertThat(beans).doesNotContain((TestBean) null);
    }

    private static class TestBean {
        private final String name;

        private TestBean(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}