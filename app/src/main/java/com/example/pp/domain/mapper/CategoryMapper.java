package com.example.pp.domain.mapper;

import com.example.pp.data.local.entity.CategoryEntity;
import com.example.pp.domain.model.CategoryTreeNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryMapper {

    public static List<CategoryTreeNode> buildTree(List<CategoryEntity> entities) {
        if (entities == null || entities.isEmpty()) return new ArrayList<>();

        Map<String, CategoryTreeNode> nodeMap = new HashMap<>();
        List<CategoryTreeNode> rootNodes = new ArrayList<>();

        // Create nodes for all entities
        for (CategoryEntity entity : entities) {
            nodeMap.put(entity.getCategoryId(), new CategoryTreeNode(entity));
        }

        // Connect children to parents
        for (CategoryEntity entity : entities) {
            CategoryTreeNode node = nodeMap.get(entity.getCategoryId());
            String parentId = entity.getParentCategoryId();

            if (parentId == null) {
                rootNodes.add(node);
            } else {
                CategoryTreeNode parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    parentNode.addChild(node);
                } else {
                    // Fallback for cases where parent might not be in the list
                    rootNodes.add(node);
                }
            }
        }

        return rootNodes;
    }
}