package com.garment.db;

import com.baomidou.mybatisplus.annotation.TableName;
import com.garment.entity.Bed;
import com.garment.entity.Bundle;
import com.garment.entity.BundleProcess;
import com.garment.entity.Company;
import com.garment.entity.CompanyRenewLog;
import com.garment.entity.Notification;
import com.garment.entity.NotificationRead;
import com.garment.entity.Process;
import com.garment.entity.ProcessTemplate;
import com.garment.entity.ProcessTemplateItem;
import com.garment.entity.Style;
import com.garment.entity.User;
import com.garment.entity.UserCompany;
import com.garment.entity.WageAdjustment;
import com.garment.entity.WageSettlement;
import com.garment.entity.WageSettlementDetail;
import com.garment.entity.WorkRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessTemplateItemSchemaTest {

    @Test
    void schemaDefinesEveryPersistedEntityField() throws IOException {
        String schema = read("src/main/resources/db/schema.sql");

        for (Class<?> entityClass : entityClasses()) {
            String tableName = entityClass.getAnnotation(TableName.class).value();
            String tableDefinition = tableDefinition(schema, tableName);

            for (Field field : entityClass.getDeclaredFields()) {
                String columnName = camelToSnake(field.getName());
                assertTrue(tableDefinition.contains("`" + columnName + "`"),
                        tableName + "." + columnName + " required by " + entityClass.getSimpleName());
            }
        }
    }

    @Test
    void migrationProvidesDeletedColumnForExistingProcessTemplateItems() throws IOException {
        String migration = read("src/main/resources/db/migration.sql");

        assertTrue(migration.contains("table_name = 'process_template_item'"));
        assertTrue(migration.contains("column_name = 'deleted'"));
        assertTrue(migration.contains("ALTER TABLE `process_template_item` ADD COLUMN `deleted`"));
    }

    @Test
    void migrationGuardsOneTimeDataBackfills() throws IOException {
        String migration = read("src/main/resources/db/migration.sql");

        assertTrue(migration.contains("@has_process_price_locked = 0,\n    'UPDATE `process`"),
                "process price locking backfill must only run when the column is first added");
        assertTrue(migration.contains("@has_notification_read_time = 0 AND @has_notification_read_create_time > 0"),
                "notification read backfill must not overwrite read_time on every deployment");
    }

    private List<Class<?>> entityClasses() {
        return List.of(
                Bed.class, Bundle.class, BundleProcess.class, Company.class,
                CompanyRenewLog.class, Notification.class, NotificationRead.class,
                Process.class, ProcessTemplate.class, ProcessTemplateItem.class,
                Style.class, User.class, UserCompany.class, WageAdjustment.class,
                WageSettlement.class, WageSettlementDetail.class, WorkRecord.class
        );
    }

    private String tableDefinition(String schema, String tableName) {
        String marker = "CREATE TABLE IF NOT EXISTS `" + tableName + "`";
        int start = schema.indexOf(marker);
        assertTrue(start >= 0, "Missing table " + tableName);
        return schema.substring(start, schema.indexOf(";", start));
    }

    private String camelToSnake(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
