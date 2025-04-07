package org.dromara.autotable.test.doris;

import org.dromara.autotable.core.AutoTableGlobalConfig;
import org.dromara.autotable.core.strategy.doris.DorisStrategy;
import org.dromara.autotable.core.strategy.doris.builder.DorisMetadataBuilder;
import org.dromara.autotable.core.strategy.doris.builder.DorisSqlBuilder;
import org.dromara.autotable.core.strategy.doris.data.DorisTableMetadata;
import org.dromara.autotable.springboot.EnableAutoTable;
import org.dromara.autotable.test.doris.entity.Table13;
import org.dromara.autotable.test.doris.entity.Table14;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 *
 * @author lizhian
 */
@EnableAutoTable
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        AutoTableGlobalConfig.addStrategy(new DorisStrategy());
        

        DorisTableMetadata dorisTableMetadata = DorisMetadataBuilder.buildTableMetadata(Table14.class);

        SpringApplication.run(Application.class, args);
    }
}
