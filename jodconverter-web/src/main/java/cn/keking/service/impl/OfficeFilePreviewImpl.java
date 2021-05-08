package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreview;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.FileUtils;
import cn.keking.utils.OfficeToPdf;
import cn.keking.utils.PdfUtils;
import cn.keking.web.filter.BaseUrlFilter;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Created by kl on 2018/1/17.
 * Content :处理office文件
 */
@Service
public class OfficeFilePreviewImpl implements FilePreview {

    private final FileUtils fileUtils;

    private final PdfUtils pdfUtils;

    private final DownloadUtils downloadUtils;

    private final OfficeToPdf officeToPdf;

    public OfficeFilePreviewImpl(FileUtils fileUtils,
                                 PdfUtils pdfUtils,
                                 DownloadUtils downloadUtils,
                                 OfficeToPdf officeToPdf) {
        this.fileUtils = fileUtils;
        this.pdfUtils = pdfUtils;
        this.downloadUtils = downloadUtils;
        this.officeToPdf = officeToPdf;
    }

    public static final String OFFICE_PREVIEW_TYPE_IMAGE = "image";
    public static final String OFFICE_PREVIEW_TYPE_ALL_IMAGES = "allImages";
    // 转换后的文件地址
    private static final String FILE_DIR = ConfigConstants.getFileDir();

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        // 预览Type，参数传了就取参数的，没传取系统默认
        String officePreviewType = model.asMap().get("officePreviewType") == null ? ConfigConstants.getOfficePreviewType() : model.asMap().get("officePreviewType").toString();
        // kkfile服务器地址
        String baseUrl = BaseUrlFilter.getBaseUrl();
        // 原文件后缀名
        String suffix=fileAttribute.getSuffix();
        // 原文件名称
        String fileName=fileAttribute.getName();
        // xls , xlsx 转成html,其余转成 pdf格式
        boolean isHtml = suffix.equalsIgnoreCase("xls") || suffix.equalsIgnoreCase("xlsx");
        String pdfName = fileName.substring(0, fileName.lastIndexOf(".") + 1) + (isHtml ? "html" : "pdf");
        // 转换后的文件路径
        String outFilePath = FILE_DIR + pdfName;

        // 先从 fileUtils.listConvertedFiles()的Map(缓存)中查找是否已经转换过,转换过的话直接返回,否则执行转换
        // ConfigConstants.isCacheEnabled() 是否启用缓存功能 默认为true
        if (!fileUtils.listConvertedFiles().containsKey(pdfName) || !ConfigConstants.isCacheEnabled()) {
            String filePath;
            // 1 下载远程端文件到 KKFile服务器
            ReturnResponse<String> response = downloadUtils.downLoad(fileAttribute, null);
            if (0 != response.getCode()) {
                model.addAttribute("fileType", suffix);
                model.addAttribute("msg", response.getMsg());
                return "fileNotSupported";
            }
            // kkfile把远程端文件下载到自己服务器后的文件路径
            filePath = response.getContent();

            if (StringUtils.hasText(outFilePath)) {
                // 核心方法: 将下载过来的office转换成pdf
                // 例如 D:\kkFileview\b937eb1a-e02f-431d-bba9-bd9a4b664fbc.docx -> D:\kkFileview\1595314467960.pdf  */
                officeToPdf.openOfficeToPDF(filePath, outFilePath);
                if (isHtml) {
                    // 对转换后的文件进行操作(改变编码方式)
                    fileUtils.doActionConvertedFile(outFilePath);
                }
                if (ConfigConstants.isCacheEnabled()) {
                    // 加入缓存
                    fileUtils.addConvertedFile(pdfName, fileUtils.getRelativePath(outFilePath));
                }
            }
        }

        // 2. 是否需要顺带转换成jpg的形式
        if (!isHtml && baseUrl != null && (OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType) || OFFICE_PREVIEW_TYPE_ALL_IMAGES.equals(officePreviewType))) {
            /** 以jpg的形式展现 */
            return getPreviewType(model, fileAttribute, officePreviewType, baseUrl, pdfName, outFilePath, pdfUtils, OFFICE_PREVIEW_TYPE_IMAGE);
        }
        model.addAttribute("pdfUrl", pdfName);
        // 跳转到 classpath:/web/下的 pdf.ftl 或 html.ftl 页面
        return isHtml ? "html" : "pdf";
    }

    static String getPreviewType(Model model, FileAttribute fileAttribute, String officePreviewType, String baseUrl, String pdfName, String outFilePath, PdfUtils pdfUtils, String officePreviewTypeImage) {
        // pdf -> jpg
        List<String> imageUrls = pdfUtils.pdf2jpg(outFilePath, pdfName, baseUrl);
        if (imageUrls == null || imageUrls.size() < 1) {
            model.addAttribute("msg", "office转图片异常，请联系管理员");
            model.addAttribute("fileType",fileAttribute.getSuffix());
            return "fileNotSupported";
        }
        model.addAttribute("imgurls", imageUrls);
        model.addAttribute("currentUrl", imageUrls.get(0));
        if (officePreviewTypeImage.equals(officePreviewType)) {
            return "officePicture";
        } else {
            return "picture";
        }
    }
}
