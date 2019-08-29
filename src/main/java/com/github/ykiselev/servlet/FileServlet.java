package com.github.ykiselev.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Yuriy Kiselev (uze@yandex.ru)
 * @since 29.08.2019
 */
public class FileServlet extends HttpServlet {

    private Path docBase;

    @Override
    public void init() throws ServletException {
        final ServletConfig cfg = getServletConfig();
        this.docBase = Paths.get(System.getProperty(cfg.getInitParameter("docBaseParameterName")));
    }

    private File getFile(HttpServletRequest req) {
        String path = req.getPathInfo();
        int i = 0;
        while (i < path.length() && path.charAt(i) == '/') {
            i++;
        }
        if (i > 0) {
            path = path.substring(i);
        }

        final Path resource = docBase.resolve(path);
        if (Files.isReadable(resource)) {
            return resource.toFile();
        }
        return null;
    }

    private String getMimeType(File file) {
        return getServletContext().getMimeType(file.getName());
    }

    private void serveFile(HttpServletRequest req, HttpServletResponse resp, boolean head) throws IOException {
        final File file = getFile(req);
        if (file == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(getMimeType(file));
        resp.addHeader("Content-Length", Long.toString(file.length()));
        if (!head) {
            Files.copy(file.toPath(), resp.getOutputStream());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        serveFile(req, resp, false);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        serveFile(req, resp, true);
    }

    private String getFileName(Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf("=") + 2, content.length() - 1);
            }
        }
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //final String contentType = req.getHeader("Content-Type");
        //final long contentLength = Long.valueOf(req.getHeader("Content-Length"));

        for (Part part : req.getParts()) {
            final String fileName = getFileName(part);
            if (fileName == null) {
                continue;
            }
            final Path target = docBase.resolve(fileName);
            Files.copy(part.getInputStream(), target);
        }
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doDelete(req, resp);
    }
}
