package cn.keking.config;

import org.artofsolving.jodconverter.office.OfficeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * 配置文件刷新 的组件
 * @auther: chenjh
 * @time: 2019/4/10 16:16
 * @description 每隔1s读取并更新一次配置文件
 */
@Component
public class ConfigRefreshComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRefreshComponent.class);

    /**
     * spring容器启动后 开启刷新配置文件的任务
     */
    @PostConstruct
    void refresh() {
        Thread configRefreshThread = new Thread(new ConfigRefreshThread());
        configRefreshThread.start();
    }

    /**
     * 每隔1s读取并更新一次配置文件
     */
    static class ConfigRefreshThread implements Runnable {
        @Override
        public void run() {
            try {
                Properties properties = new Properties();
                String text;
                String media;
                boolean cacheEnabled;
                String[] textArray;
                String[] mediaArray;
                String officePreviewType;
                String ftpUsername;
                String ftpPassword;
                String ftpControlEncoding;
                String configFilePath = OfficeUtils.getCustomizedConfigPath(); //配置文件路径
                String baseUrl;
                String trustHost;
                String pdfDownloadDisable;
                while (true) {
                    LOGGER.info("更新配置信息");
                    // 读取配置文件
                    FileReader fileReader = new FileReader(configFilePath);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    properties.load(bufferedReader);

                    // 核心方法: 获取配置文件中的属性并转换
                    OfficeUtils.restorePropertiesFromEnvFormat(properties);
                    // 优先从配置文件中读取属性,空的话再设置为默认值
                    cacheEnabled = Boolean.parseBoolean(properties.getProperty("cache.enabled", ConfigConstants.DEFAULT_CACHE_ENABLED));
                    text = properties.getProperty("simText", ConfigConstants.DEFAULT_TXT_TYPE);
                    media = properties.getProperty("media", ConfigConstants.DEFAULT_MEDIA_TYPE);
                    officePreviewType = properties.getProperty("office.preview.type", ConfigConstants.DEFAULT_OFFICE_PREVIEW_TYPE);
                    ftpUsername = properties.getProperty("ftp.username", ConfigConstants.DEFAULT_FTP_USERNAME);
                    ftpPassword = properties.getProperty("ftp.password", ConfigConstants.DEFAULT_FTP_PASSWORD);
                    ftpControlEncoding = properties.getProperty("ftp.control.encoding", ConfigConstants.DEFAULT_FTP_CONTROL_ENCODING);
                    textArray = text.split(",");
                    mediaArray = media.split(",");
                    baseUrl = properties.getProperty("base.url", ConfigConstants.DEFAULT_BASE_URL);
                    trustHost = properties.getProperty("trust.host", ConfigConstants.DEFAULT_TRUST_HOST);
                    pdfDownloadDisable = properties.getProperty("pdf.download.disable", ConfigConstants.DEFAULT_PDF_DOWNLOAD_DISABLE);

                    // 重置 配置文件参数
                    ConfigConstants.setCacheEnabledValueValue(cacheEnabled);
                    ConfigConstants.setSimTextValue(textArray);
                    ConfigConstants.setMediaValue(mediaArray);
                    ConfigConstants.setOfficePreviewTypeValue(officePreviewType);
                    ConfigConstants.setFtpUsernameValue(ftpUsername);
                    ConfigConstants.setFtpPasswordValue(ftpPassword);
                    ConfigConstants.setFtpControlEncodingValue(ftpControlEncoding);
                    ConfigConstants.setBaseUrlValue(baseUrl);
                    ConfigConstants.setTrustHostValue(trustHost);
                    ConfigConstants.setPdfDownloadDisableValue(pdfDownloadDisable);
                    // 设置水印配置文件
                    setWatermarkConfig(properties);
                    bufferedReader.close();
                    fileReader.close();
                    // 每隔1s读取并更新一次配置文件
                    Thread.sleep(1000L);
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("读取配置文件异常", e);
            }
        }

        /**
         * 设置水印配置文件
         * @param properties
         */
        private void setWatermarkConfig(Properties properties) {
            String watermarkTxt = properties.getProperty("watermark.txt", WatermarkConfigConstants.DEFAULT_WATERMARK_TXT);
            String watermarkXSpace = properties.getProperty("watermark.x.space", WatermarkConfigConstants.DEFAULT_WATERMARK_X_SPACE);
            String watermarkYSpace = properties.getProperty("watermark.y.space", WatermarkConfigConstants.DEFAULT_WATERMARK_Y_SPACE);
            String watermarkFont = properties.getProperty("watermark.font", WatermarkConfigConstants.DEFAULT_WATERMARK_FONT);
            String watermarkFontsize = properties.getProperty("watermark.fontsize", WatermarkConfigConstants.DEFAULT_WATERMARK_FONTSIZE);
            String watermarkColor = properties.getProperty("watermark.color", WatermarkConfigConstants.DEFAULT_WATERMARK_COLOR);
            String watermarkAlpha = properties.getProperty("watermark.alpha", WatermarkConfigConstants.DEFAULT_WATERMARK_ALPHA);
            String watermarkWidth = properties.getProperty("watermark.width", WatermarkConfigConstants.DEFAULT_WATERMARK_WIDTH);
            String watermarkHeight = properties.getProperty("watermark.height", WatermarkConfigConstants.DEFAULT_WATERMARK_HEIGHT);
            String watermarkAngle = properties.getProperty("watermark.angle", WatermarkConfigConstants.DEFAULT_WATERMARK_ANGLE);
            WatermarkConfigConstants.setWatermarkTxtValue(watermarkTxt);
            WatermarkConfigConstants.setWatermarkXSpaceValue(watermarkXSpace);
            WatermarkConfigConstants.setWatermarkYSpaceValue(watermarkYSpace);
            WatermarkConfigConstants.setWatermarkFontValue(watermarkFont);
            WatermarkConfigConstants.setWatermarkFontsizeValue(watermarkFontsize);
            WatermarkConfigConstants.setWatermarkColorValue(watermarkColor);
            WatermarkConfigConstants.setWatermarkAlphaValue(watermarkAlpha);
            WatermarkConfigConstants.setWatermarkWidthValue(watermarkWidth);
            WatermarkConfigConstants.setWatermarkHeightValue(watermarkHeight);
            WatermarkConfigConstants.setWatermarkAngleValue(watermarkAngle);

        }
    }
}
