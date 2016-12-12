package com.atex.plugins.collaboration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.polopoly.cm.app.policy.CheckboxPolicy;
import com.polopoly.cm.client.CMException;

/**
 * Unit test for {@link CollaborationConfigPolicy}.
 *
 * @author mnova
 */
@RunWith(MockitoJUnitRunner.class)
public class CollaborationConfigPolicyTest {

    @Mock
    CollaborationConfigPolicy policy;

    @Mock
    CheckboxPolicy enabled;

    @Before
    public void before() throws CMException {
        Mockito
                .when(policy.getChildPolicy("enabled"))
                .thenReturn(enabled);
        Mockito.when(policy.getWebHookUrl()).thenCallRealMethod();
        Mockito.when(policy.isEnabled()).thenCallRealMethod();
    }

    @Test
    public void testWebHookUrlNull() {
        Mockito
                .when(policy.getChildValue(
                        Mockito.eq("webHookUrl"),
                        Mockito.anyString()))
                .thenReturn(null);

        Assert.assertNull(policy.getWebHookUrl());
    }

    @Test
    public void testWebHookUrlValidValue() {
        Mockito
                .when(policy.getChildValue(
                        Mockito.eq("webHookUrl"),
                        Mockito.anyString()))
                .thenReturn("hello");

        Assert.assertEquals("hello", policy.getWebHookUrl());
    }

    @Test
    public void testEnabled() throws CMException {
        Mockito
                .when(enabled.getChecked())
                .thenReturn(true);
        Mockito
                .when(policy.getChildValue(
                        Mockito.eq("webHookUrl"),
                        Mockito.anyString()))
                .thenReturn("hello");

        Assert.assertEquals(true, policy.isEnabled());
    }

    @Test
    public void testDisabled() throws CMException {
        Mockito
                .when(enabled.getChecked())
                .thenReturn(false);

        Assert.assertEquals(false, policy.isEnabled());
    }

    @Test
    public void testDisabledWhenWebHookUrlIsNull() throws CMException {
        Mockito
                .when(enabled.getChecked())
                .thenReturn(true);
        Mockito
                .when(policy.getChildValue(
                        Mockito.eq("webHookUrl"),
                        Mockito.anyString()))
                .thenReturn(null);

        Assert.assertEquals(false, policy.isEnabled());
    }

    @Test
    public void testDisabledWhenWebHookUrlIsEmpty() throws CMException {
        Mockito
                .when(enabled.getChecked())
                .thenReturn(true);
        Mockito
                .when(policy.getChildValue(
                        Mockito.eq("webHookUrl"),
                        Mockito.anyString()))
                .thenReturn("");

        Assert.assertEquals(false, policy.isEnabled());
    }

}