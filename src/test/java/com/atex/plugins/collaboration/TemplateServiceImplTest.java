package com.atex.plugins.collaboration;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@RunWith(MockitoJUnitRunner.class)
public class TemplateServiceImplTest {

    private TemplateService service = new TemplateServiceImpl();

    @Test
    public void testSimpleScope() throws IOException {
        final String templateName = "/mustache/simpleScope.hbs";
        final Map<String, String> scope = Maps.newHashMap();
        scope.put("name", "marco");

        final String result = service.execute(templateName, scope);
        Assert.assertEquals("Hello marco", result);
    }

    @Test
    public void testComplexScope() throws IOException {
        final String templateName = "/mustache/complexScope.hbs";
        final UserList userList = new UserList();
        userList.users.add(new User("user1", "user1@email.it"));
        userList.users.add(new User("user2", "user2@email.it"));
        userList.users.add(new User("user3", "user3@email.it"));
        final Site site = new Site("arena");

        final String result = service.execute(templateName, new Object[] { site, userList });
        final String expected = "Hello arena\n" +
                "<b>user1-user1@email.it</b>\n" +
                "<b>user2-user2@email.it</b>\n" +
                "<b>user3-user3@email.it</b>\n";
        Assert.assertEquals(expected, result);
    }

    private class User {
        private String name;
        private String email;

        public User(final String name, final String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }

    private class UserList {
        private List<User> users = Lists.newArrayList();

        public List<User> getUsers() {
            return users;
        }
    }

    private class Site {
        private String name;

        public Site(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}