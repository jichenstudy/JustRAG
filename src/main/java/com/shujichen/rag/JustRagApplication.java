package com.shujichen.rag;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 启动程序
 */
@SpringBootApplication
@MapperScan("com.shujichen.rag.mapper")
public class JustRagApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(JustRagApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  JustRAG启动成功   ლ(´ڡ`ლ)ﾞ");
    }

}
