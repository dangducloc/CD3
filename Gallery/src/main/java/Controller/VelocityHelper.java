package Controller;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.util.Properties;

import javax.servlet.ServletContext;

public class VelocityHelper {
    private VelocityEngine velocityEngine;

    public VelocityHelper(ServletContext context) {
        velocityEngine = new VelocityEngine();
        Properties props = new Properties();

        // Load template bằng đường dẫn thực trong server
        String realPath = context.getRealPath("/WEB-INF/templates");
        props.setProperty("resource.loader", "file");
        props.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
        props.setProperty("file.resource.loader.path", realPath);

        velocityEngine.init(props);
    }

    public String renderTemplate(String templateName, VelocityContext context) {
        Template template = velocityEngine.getTemplate(templateName, "UTF-8");
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }
}
