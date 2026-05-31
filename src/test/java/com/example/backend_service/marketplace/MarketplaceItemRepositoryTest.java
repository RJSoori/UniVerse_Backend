package com.example.backend_service.marketplace;

import com.example.backend_service.marketplace.enums.ItemCondition;
import com.example.backend_service.marketplace.enums.ItemType;
import com.example.backend_service.marketplace.model.MarketplaceItem;
import com.example.backend_service.marketplace.repository.MarketplaceItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class MarketplaceItemRepositoryTest {

    @Autowired
    private MarketplaceItemRepository repository;

    @Test
    void saveAndFindBySellerId() {
        MarketplaceItem item = new MarketplaceItem();
        item.setItemName("Laptop");
        item.setDescription("Lightweight laptop");
        item.setPrice(499.99);
        // required enums
        item.setType(ItemType.SELL);
        item.setCondition(ItemCondition.GOOD);
        repository.save(item);

        // since no seller was set, findBySellerId should return empty for a random id
        List<MarketplaceItem> list = repository.findBySellerId(123L);
        assertThat(list).isEmpty();

        // basic save verify
        List<MarketplaceItem> all = repository.findAll();
        assertThat(all).isNotEmpty();
        assertThat(all.get(0).getItemName()).isEqualTo("Laptop");
    }
}
