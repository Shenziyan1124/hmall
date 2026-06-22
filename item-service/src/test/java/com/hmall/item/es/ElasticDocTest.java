package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.dto.ItemDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticDocTest {
    private RestHighLevelClient client;

    @Autowired
    private IItemService itemService;

    @Test
    void testIndexConnection() throws IOException {

        Item item = itemService.getById(317578L);
        ItemDTO itemDTO = BeanUtil.copyProperties(item, ItemDTO.class);

        IndexRequest request = new IndexRequest("items").id(String.valueOf(itemDTO.getId()));
        request.source(JSONUtil.toJsonStr(itemDTO), XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testIndexDeleteDocumentById() throws IOException {
        DeleteRequest items = new DeleteRequest("items", "317578");
        client.delete(items, RequestOptions.DEFAULT);
    }

    @Test
    void testIndexGetDocumentById() throws IOException {
        GetRequest request = new GetRequest("items", "317578");
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        String sourceAsString = response.getSourceAsString();
        System.out.println(sourceAsString);
    }

    @Test
    void testUpdateDocumentById() throws IOException {
        UpdateRequest request = new UpdateRequest("items", "317578");
        request.doc(
                "price", 10
        );
        client.update(request, RequestOptions.DEFAULT);
    }

    @Test
    void testBulk() throws IOException {
        int  pageNum = 1, pageSize = 500;

        while (true){
            Page<Item> page = itemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(Page.of(pageNum, pageSize));
            List<Item> records = page.getRecords();
            if ( records == null || records.isEmpty()){ return;}

            BulkRequest request = new BulkRequest();
            for (Item item : records){
                request.add(new IndexRequest("items")
                        .id(item.getId().toString())
                        .source(JSONUtil.toJsonStr(BeanUtil.copyProperties(item, ItemDTO.class)),XContentType.JSON));
            }

            client.bulk(request, RequestOptions.DEFAULT);
            pageNum++;
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
