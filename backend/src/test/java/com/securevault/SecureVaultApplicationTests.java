package com.securevault;

import com.securevault.config.TestMailConfig;
import com.securevault.security.EncryptionService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@SpringBootTest(
        classes = {
                SecureVaultApplication.class,
                TestMailConfig.class
        },
        properties = "spring.mail.username=test@securevault.local"
)
class SecureVaultApplicationTests {

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private DataSource dataSource;

    @Test
    void testUser27Credentials() throws Exception {

        String sql =
                "SELECT id, password " +
                "FROM credentials " +
                "WHERE user_id = 27 " +
                "ORDER BY id";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {

                long id =
                        resultSet.getLong("id");

                String encryptedPassword =
                        resultSet.getString("password");

                try {

                    encryptionService.decrypt(
                            encryptedPassword
                    );

                    System.out.println(
                            "ID " + id +
                            ": DECRYPT SUCCESS"
                    );

                } catch (Exception e) {

                    System.out.println(
                            "ID " + id +
                            ": DECRYPT FAILED"
                    );
                }
            }
        }
    }
}
