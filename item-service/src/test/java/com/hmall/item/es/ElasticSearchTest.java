package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.dto.ItemDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticSearchTest {
    private RestHighLevelClient client;

    @Test
    void testAgg() throws IOException {
        SearchRequest request = new SearchRequest("items");

        request.source().size(0); // 不返回结果

        String aggName = "brand_agg";
        request.source().aggregation(
                AggregationBuilders.terms(aggName).field("brand.keyword").size(10)
        );

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // 解析
        Aggregations aggregations = response.getAggregations();
        Terms terms = aggregations.get(aggName);
        terms.getBuckets().forEach(bucket -> {
            System.out.println( "key:" + bucket.getKeyAsString());
            System.out.println( "count:" + bucket.getDocCount());
        });

    }

    @Test
    void testHighLevelClient() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(QueryBuilders.matchQuery("name.keyword", "脱脂牛奶"));
        request.source().highlighter(SearchSourceBuilder.highlight().field("name").preTags("<em>").postTags("</em>"));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }

    @Test
    void testSortAndPage() throws IOException {
        int pageNum = 1 , pageSize = 5;

        SearchRequest request = new SearchRequest("items");
        request.source().query(QueryBuilders.matchAllQuery());
        request.source().from( (pageNum - 1) * pageSize ).size(pageSize);
        request.source().sort("price", SortOrder.DESC);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }


    @Test
    void testBoolQuery() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(
                QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name", "脱脂牛奶"))
                        .filter(QueryBuilders.rangeQuery("price").lt(30000))
                        .filter(QueryBuilders.termQuery("brand.keyword", "德亚"))
        );
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponseResult(response);
    }


    @Test
    void testMatchAll() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(QueryBuilders.matchAllQuery());
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // 4. 解析结果
        parseResponseResult(response);

    }

    private static void parseResponseResult(SearchResponse response) {
        SearchHits hits = response.getHits();
        Assertions.assertNotNull(hits.getTotalHits());
        long total = hits.getTotalHits().value;
        SearchHit[] hitsHits = hits.getHits();
        for (SearchHit hit : hitsHits){
            String sourceAsString = hit.getSourceAsString();
            ItemDoc doc = JSONUtil.toBean(sourceAsString, ItemDoc.class);

            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null && !highlightFields.isEmpty()){
                HighlightField hf = highlightFields.get("name");
                doc.setName(hf.fragments()[0].toString());
            }
            System.out.println("doc = " + doc);
        }
    }

    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("192.168.31.188", 9200, "http")
                )
        );
    }

    @AfterEach
    void tearDown() {
        if (client != null){
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
