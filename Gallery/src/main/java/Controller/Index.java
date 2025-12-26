package Controller;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;

import Utils.Pool;
import Model.Img;

import org.apache.velocity.VelocityContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@WebServlet("/")
public class Index extends HttpServlet {
    private static final Logger logger = LogManager.getLogger(Index.class);
    private Pool pool = new Pool();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, java.io.IOException {

        // Khởi tạo Velocity với đúng đường dẫn template
        VelocityHelper velocityHelper = new VelocityHelper(req.getServletContext());

        ArrayList<Img> images = null;
        String searchQuery = req.getParameter("search");

        try {
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                images = pool.searchImages(searchQuery.trim());
                logger.info("Search query: {}", searchQuery);
            } else {
                images = pool.getAllImages();
            }
        } catch (SQLException e) {
            logger.error("Error fetching images from the database", e);
            throw new RuntimeException(e);
        }

        // Log
        for (Img img : images) {
            
            System.out.println("===========================fetched image==================================");
            logger.info("Image ID: {}, Name by User: {}, Name on Server: {}",
                    img.getId(), img.getName_by_user(), img.getName_on_server());
        }

        // Context -> template
        VelocityContext context = new VelocityContext();
        context.put("images", images);
        context.put("searchQuery", searchQuery != null ? searchQuery : "");

        String renderedHtml = velocityHelper.renderTemplate("index.vm", context);

        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();
        writer.write(renderedHtml);
        writer.flush();
    }
}

