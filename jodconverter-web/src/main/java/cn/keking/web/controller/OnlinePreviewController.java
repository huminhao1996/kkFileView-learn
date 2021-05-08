package cn.keking.web.controller;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.service.FilePreview;
import cn.keking.service.FilePreviewFactory;

import cn.keking.service.cache.CacheService;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

/**
 * 文件转换,文件预览
 * @author yudian-it
 */
@Controller
public class OnlinePreviewController {

    private final Logger logger = LoggerFactory.getLogger(OnlinePreviewController.class);

    private final FilePreviewFactory previewFactory;

    private final CacheService cacheService;

    private final FileUtils fileUtils;

    private final DownloadUtils downloadUtils;

    public OnlinePreviewController(FilePreviewFactory filePreviewFactory,
                                   FileUtils fileUtils,
                                   CacheService cacheService,
                                   DownloadUtils downloadUtils) {
        this.previewFactory = filePreviewFactory;
        this.fileUtils = fileUtils;
        this.cacheService = cacheService;
        this.downloadUtils = downloadUtils;
    }

    /**
     * 文件预览
     * @param url
     * @param model
     * @param req
     * @return
     */
    @RequestMapping(value = "/onlinePreview")
    public String onlinePreview(String url, Model model, HttpServletRequest req) {
        // 解析文件属性
        FileAttribute fileAttribute = fileUtils.getFileAttribute(url);
        req.setAttribute("fileKey", req.getParameter("fileKey"));
        model.addAttribute("pdfDownloadDisable", ConfigConstants.getPdfDownloadDisable()); // true : 禁止下载转换生成的pdf文件
        model.addAttribute("officePreviewType", req.getParameter("officePreviewType")); // 转换后的文件展示的类型  默认为图片(image),可配置为pdf
        // 获取文件格式转换Service
        FilePreview filePreview = previewFactory.get(fileAttribute);
        logger.info("预览文件url：{}，previewType：{}", url, fileAttribute.getType());
        // 核心方法: 文件类型转换,预览
        return filePreview.filePreviewHandle(url, model, fileAttribute);
    }

    /**
     * 文件预览 仅针对图片
     * @param model
     * @param req
     * @return
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = "picturesPreview")
    public String picturesPreview(Model model, HttpServletRequest req) throws UnsupportedEncodingException {
        String urls = req.getParameter("urls");
        String currentUrl = req.getParameter("currentUrl");
        logger.info("预览文件url：{}，urls：{}", currentUrl, urls);
        // 路径转码
        String decodedUrl = URLDecoder.decode(urls, "utf-8");
        String decodedCurrentUrl = URLDecoder.decode(currentUrl, "utf-8");
        // 抽取文件并返回文件列表
        String[] imgs = decodedUrl.split("\\|");
        List imgurls = Arrays.asList(imgs);
        model.addAttribute("imgurls", imgurls);
        model.addAttribute("currentUrl",decodedCurrentUrl);
        return "picture";
    }

    /**
     * 根据url获取文件内容
     * 当pdfjs读取存在跨域问题的文件时将通过此接口读取
     *
     * @param urlPath url
     * @param response response
     */
    @RequestMapping(value = "/getCorsFile", method = RequestMethod.GET)
    public void getCorsFile(String urlPath, HttpServletResponse response) {
        logger.info("下载跨域pdf文件url：{}", urlPath);
        try {
            downloadUtils.saveToOutputStreamFromUrl(urlPath, response.getOutputStream());
        } catch (IOException e) {
            logger.error("下载跨域pdf文件异常，url：{}", urlPath, e);
        }
    }

    /**
     * 通过api接口入队
     * @param url 请编码后在入队
     */
    @RequestMapping("/addTask")
    @ResponseBody
    public String addQueueTask(String url) {
        logger.info("添加转码队列url：{}", url);
        cacheService.addQueueTask(url);
        return "success";
    }

}
