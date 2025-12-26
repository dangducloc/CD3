package Controller;

import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import Utils.Pool;
import org.apache.velocity.VelocityContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@WebServlet("/upload")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024, // 1 MB
    maxFileSize = 1024 * 1024 * 10, // 10 MB
    maxRequestSize = 1024 * 1024 * 15 // 15 MB
)
public class Upload extends HttpServlet {
    private static final Logger logger = LogManager.getLogger(Upload.class);
    private final Pool pool = new Pool();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        VelocityHelper velocityHelper = new VelocityHelper(req.getServletContext());
        VelocityContext context = new VelocityContext();

        String renderedHtml = velocityHelper.renderTemplate("upload.vm", context);

        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write(renderedHtml);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Part filePart = req.getPart("image");
        if (filePart == null || filePart.getSize() == 0) {
            resp.sendRedirect(req.getContextPath() + "/upload?error=No file uploaded");
            return;
        }

        String submittedFileName = filePart.getSubmittedFileName();
        if (submittedFileName == null || submittedFileName.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/upload?error=Invalid file name");
            return;
        }

        // Get custom name from form
        String customName = req.getParameter("customName");
        if (customName == null || customName.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/upload?error=Please enter a name for the image");
            return;
        }
        customName = customName.trim();

        // Check file size (duplicate with @MultipartConfig, but ok)
        if (filePart.getSize() > 10 * 1024 * 1024) {
            resp.sendRedirect(req.getContextPath() + "/upload?error=File too large! Maximum 10MB");
            return;
        }

        // === LỖ HỔNG CHÍNH ===
        // BỎ kiểm tra Content-Type để dễ bypass
        // Và thêm .jsp vào whitelist extension → cho phép upload JSP shell
        String ext = getFileExtension(submittedFileName).toLowerCase();
        if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") &&
            !ext.equals(".gif") && !ext.equals(".webp") && !ext.equals(".jsp")) {
            resp.sendRedirect(req.getContextPath() + "/upload?error=Only jpg, png, gif, webp, jsp allowed");
            return;
        }
        // ====================

        // Generate MD5 hash for server-side filename
        String serverFileName;
        try (InputStream inputStream = filePart.getInputStream()) {
            serverFileName = generateMD5(inputStream) + "_" + System.currentTimeMillis() + ext;  // dùng ext đã check ở trên, thêm timestamp để tránh trùng
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error generating MD5 hash", e);
            resp.sendRedirect(req.getContextPath() + "/upload?error=File processing error");
            return;
        }

        // Save file to imgs directory
        String uploadPath = getServletContext().getRealPath("/imgs");
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        Path filePath = uploadDir.resolve(serverFileName);
        try (InputStream inputStream = filePart.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Error saving file to disk", e);
            resp.sendRedirect(req.getContextPath() + "/upload?error=Failed to save file");
            return;
        }

        // Insert into database with custom name
        try {
            pool.insertImageWithCustomName(customName, serverFileName);
            logger.info("File uploaded: {} -> {} (custom name: {})", submittedFileName, serverFileName, customName);
        } catch (SQLException e) {
            logger.error("Error inserting image into database", e);
            resp.sendRedirect(req.getContextPath() + "/upload?error=Failed to save image info");
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/upload?success=1");
    }

    private String generateMD5(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        byte[] hashBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex);
    }
}