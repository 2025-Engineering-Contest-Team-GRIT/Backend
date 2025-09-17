package grit.guidance.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

    @Value("${spring.ai.vectorstore.qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.port:6333}")
    private int qdrantPort;

    @Value("${spring.ai.vectorstore.qdrant.collection:test_collection}")
    private String collectionName;

    // Spring AI의 자동 설정을 사용하므로 별도 빈 정의 제거
    // QdrantConfig는 설정 정보만 관리
    public String getQdrantHost() {
        return qdrantHost;
    }

    public int getQdrantPort() {
        return qdrantPort;
    }

    public String getCollectionName() {
        return collectionName;
    }
}
