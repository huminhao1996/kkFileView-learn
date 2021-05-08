package cn.keking.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * pdf 工具类
 */
@Component
public class PdfUtils {

    private final Logger logger = LoggerFactory.getLogger(PdfUtils.class);

    private final FileUtils fileUtils;

    @Value("${server.tomcat.uri-encoding:UTF-8}")
    private String uriEncoding;

    public PdfUtils(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    /**
     * pdf -> jpg
     * todo 优化: 分片 异步 去转换图片
     * @param pdfFilePath pdf文件路径
     * @param pdfName     pdf文件名
     * @param baseUrl     kkfile服务器地址
     * @return
     */
    public List<String> pdf2jpg(String pdfFilePath, String pdfName, String baseUrl) {
        // 图片地址List
        List<String> imageUrls = new ArrayList<>();
        // 图片总数
        Integer imageCount = fileUtils.getConvertedPdfImage(pdfFilePath);
        // 图片后缀
        String imageFileSuffix = ".jpg";
        // 图片存储的目录
        String pdfFolder = pdfName.substring(0, pdfName.length() - 4);
        // 图片前缀地址
        String urlPrefix = null;
        try {
            urlPrefix = baseUrl + URLEncoder.encode(URLEncoder.encode(pdfFolder, uriEncoding).replaceAll("\\+", "%20"), uriEncoding);
        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException", e);
            urlPrefix = baseUrl + pdfFolder;
        }

        // 1. imageCount>0的话 说明这个文件已经转换过,直接返回图片路径List
        if (imageCount != null && imageCount > 0) {
            for (int i = 0; i < imageCount ; i++)
            imageUrls.add(urlPrefix + "/" + i + imageFileSuffix);
            return imageUrls;
        }

        // 2. 核心方法: pdf ->jpg 的转换
        try {
            File pdfFile = new File(pdfFilePath);
            //读取 pdf文件
            PDDocument doc = PDDocument.load(pdfFile);
            // pdf 总页数
            int pageCount = doc.getNumberOfPages();
            // PDF渲染类
            PDFRenderer pdfRenderer = new PDFRenderer(doc);
            int index = pdfFilePath.lastIndexOf(".");
            String folder = pdfFilePath.substring(0, index);
            // 创建 图片存储的目录
            File path = new File(folder);
            if (!path.exists()) {
                path.mkdirs();
            }
            String imageFilePath;
            // pdf的每一页都转换为 jpg
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                // 图片文件路径
                imageFilePath = folder + File.separator + pageIndex + imageFileSuffix;
                // 图片流
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 105, ImageType.RGB);
                // 将流写入到文件中 dpi表示图片质量
                ImageIOUtil.writeImage(image, imageFilePath, 105);
                imageUrls.add(urlPrefix + "/" + pageIndex + imageFileSuffix);
            }
            doc.close();

            // 3. 添加转换后图片组缓存
            fileUtils.addConvertedPdfImage(pdfFilePath, pageCount);
        } catch (IOException e) {
            logger.error("Convert pdf to jpg exception, pdfFilePath：{}", pdfFilePath, e);
        }
        return imageUrls;
    }
}
