package com.atex.plugins.collaboration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.UUID;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

/**
 * Implementation of {@link TemplateService} using mustache.
 *
 * @author mnova
 */
public class TemplateServiceImpl implements TemplateService {

    @Override
    public Writer execute(final Writer writer, final String templateName, final Object scope) throws IOException {
        try (final Reader reader = getTemplate(templateName)) {
            return execute_internal(writer, reader, templateName, scope);
        }
    }

    @Override
    public Writer execute(final Writer writer, final String templateName, final Object[] scopes) throws IOException {
        try (final Reader reader = getTemplate(templateName)) {
            return execute_internal(writer, reader, templateName, scopes);
        }
    }

    @Override
    public String execute(final String templateName, final Object scope) throws IOException {
        return execute(new StringWriter(), templateName, scope).toString();
    }

    @Override
    public String execute(final String templateName, final Object[] scopes) throws IOException {
        return execute(new StringWriter(), templateName, scopes).toString();
    }

    @Override
    public String execute(final Reader reader, final Object scope) throws IOException {
        return execute_internal(new StringWriter(), reader, UUID.randomUUID().toString(), scope).toString();
    }

    @Override
    public String execute(final Reader reader, final Object[] scopes) throws IOException {
        return execute_internal(new StringWriter(), reader, UUID.randomUUID().toString(), scopes).toString();
    }

    private Writer execute_internal(final Writer writer, final Reader reader, final String templateName, final Object scope) throws IOException {
        final MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(reader, templateName);
        mustache.execute(writer, scope);
        writer.flush();
        return writer;
    }

    private Writer execute_internal(final Writer writer, final Reader reader, final String templateName, final Object[] scopes) throws IOException {
        final MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(reader, templateName);
        mustache.execute(writer, scopes);
        writer.flush();
        return writer;
    }

    private Reader getTemplate(final String templateName) {
        final InputStream stream = this.getClass().getResourceAsStream(templateName);
        return new InputStreamReader(stream, Charset.forName("UTF-8"));
    }
}
