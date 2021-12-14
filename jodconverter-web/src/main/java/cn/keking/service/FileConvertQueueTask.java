package cn.keking.service;

import cn.keking.model.FileAttribute;
import cn.keking.model.FileType;
import cn.keking.service.cache.CacheService;
import cn.keking.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.ExtendedModelMap;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by kl on 2018/1/19.
 * Content :消费队列中的转换文件
 */
@Service
public class FileConvertQueueTask {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FilePreviewFactory previewFactory;

    private final CacheService cacheService;

    private final FileUtils fileUtils;

    public FileConvertQueueTask(FilePreviewFactory previewFactory,
                                CacheService cacheService,
                                FileUtils fileUtils) {
        this.previewFactory = previewFactory;
        this.cacheService = cacheService;
        this.fileUtils = fileUtils;
    }

    /**
     * spring容器启动后 开始从消费队列中获取文件并转换
     */
    @PostConstruct
    public void startTask() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        // 执行文件转换任务
        executorService.submit(new ConvertTask(previewFactory, cacheService, fileUtils));
        logger.info("队列处理文件转换任务启动完成 ");
    }

    /**
     * 转换任务
     */
    static class ConvertTask implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(ConvertTask.class);

        private final FilePreviewFactory previewFactory;

        private final CacheService cacheService;

        private final FileUtils fileUtils;

        public ConvertTask(FilePreviewFactory previewFactory,
                           CacheService cacheService,
                           FileUtils fileUtils) {
            this.previewFactory = previewFactory;
            this.cacheService = cacheService;
            this.fileUtils = fileUtils;
        }

        @Override
        public void run() {
            while (true) {
                String url = null;
                try {
                    // 从缓存中获取 待转换队列
                    url = cacheService.takeQueueTask();
                    if (url != null) {
                        // 文件类型转换,实现逻辑和文件预览大部分一致
                        FileAttribute fileAttribute = fileUtils.getFileAttribute(url);
                        FileType fileType = fileAttribute.getType();
                        logger.info("正在处理预览转换任务，url：{}，预览类型：{}", url, fileType);
                        if (fileType.equals(FileType.compress) || fileType.equals(FileType.office) || fileType.equals(FileType.cad) || fileType.equals(FileType.pdf)) {
                            FilePreview filePreview = previewFactory.get(fileAttribute);
                            filePreview.filePreviewHandle(url, new ExtendedModelMap(), fileAttribute);
                        } else {
                            logger.info("预览类型无需处理，url：{}，预览类型：{}", url, fileType);
                        }
                    }
                } catch (Exception e) {
                    // 出现异常后 休眠10秒
                    try {
                        Thread.sleep(1000 * 10);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    logger.info("处理预览转换任务异常，url：{}", url, e);
                }
            }
        }
    }

}
