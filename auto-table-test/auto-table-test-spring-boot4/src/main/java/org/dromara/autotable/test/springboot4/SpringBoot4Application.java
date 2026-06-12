package org.dromara.autotable.test.springboot4;

import org.dromara.autotable.springboot.EnableAutoTable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 4 测试启动类
 * @author don
 */
@EnableAutoTable
@SpringBootApplication
public class SpringBoot4Application {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot4Application.class, args);
    }
}
