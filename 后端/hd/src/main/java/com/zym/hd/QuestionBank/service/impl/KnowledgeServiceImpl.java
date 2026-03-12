package com.zym.hd.QuestionBank.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zym.hd.QuestionBank.entity.KnowledgeEntity;
import com.zym.hd.QuestionBank.mapper.KnowledgeMapper;
import com.zym.hd.QuestionBank.service.KnowledgeService;

import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, KnowledgeEntity> implements KnowledgeService {

    @Override
    public String getKnowledgeTreeJson(Long courseId) {
        List<KnowledgeEntity> all = baseMapper.selectByCourseId(courseId);
        Map<Long, TreeNode> nodeMap = new HashMap<>();
        List<TreeNode> roots = new ArrayList<>();

        for (KnowledgeEntity item : all) {
            TreeNode node = new TreeNode(item.getId(), item.getName());
            nodeMap.put(item.getId(), node);
        }
        for (KnowledgeEntity item : all) {
            TreeNode current = nodeMap.get(item.getId());
            if (item.getParentId() == null || item.getParentId() == 0) {
                roots.add(current);
                continue;
            }
            TreeNode parent = nodeMap.get(item.getParentId());
            if (parent == null) {
                roots.add(current);
            } else {
                parent.children.add(current);
            }
        }
        return toJsonArray(roots);
    }

    @Override
    public int seedComputerScienceTree(Long courseId) {
        List<SeedNode> roots = buildSeedTree();
        int[] sort = new int[]{1};
        long maxCourseId = baseMapper.selectMaxCourseId() == null ? 0L : baseMapper.selectMaxCourseId();
        long currentCourseId = (courseId == null || courseId <= 0) ? (maxCourseId + 1) : Math.max(courseId, maxCourseId + 1);
        for (SeedNode root : roots) {
            // root node (parent_id=null) uses incremented course_id
            persistNode(currentCourseId, null, "", root, 1, sort);
            currentCourseId++;
        }
        return sort[0] - 1;
    }

    @Override
    public boolean createKnowledgePoint(KnowledgeEntity entity) {
        if (entity == null || entity.getCourseId() == null) {
            throw new IllegalArgumentException("courseId is required");
        }
        if (!StringUtils.hasText(entity.getName())) {
            throw new IllegalArgumentException("name is required");
        }

        KnowledgeEntity parent = null;
        Long parentId = normalizeParentId(entity.getParentId());
        if (parentId != null) {
            parent = getById(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("parent knowledge point not found");
            }
            if (!Objects.equals(parent.getCourseId(), entity.getCourseId())) {
                throw new IllegalArgumentException("parent knowledge point does not belong to current course");
            }
        }

        entity.setId(null);
        entity.setParentId(parentId);
        entity.setLevel(parent == null ? 1 : defaultLevel(parent.getLevel()) + 1);
        entity.setSortNo(nextSortNo(entity.getCourseId()));
        entity.setPath(null);

        boolean saved = save(entity);
        if (!saved) {
            return false;
        }

        String parentPath = parent == null ? "" : parent.getPath();
        entity.setPath(buildPath(parentPath, entity.getId()));
        return updateById(entity);
    }

    private void persistNode(Long courseId, Long parentId, String parentPath, SeedNode seed, int level, int[] sort) {
        KnowledgeEntity entity = new KnowledgeEntity();
        entity.setCourseId(courseId);
        entity.setParentId(parentId);
        entity.setName(seed.name);
        entity.setLevel(level);
        entity.setSortNo(sort[0]++);
        save(entity);

        Long currentId = entity.getId();
        String path = parentPath == null || parentPath.isEmpty() ? String.valueOf(currentId) : parentPath + "/" + currentId;
        entity.setPath(path);
        updateById(entity);

        for (SeedNode child : seed.children) {
            persistNode(courseId, currentId, path, child, level + 1, sort);
        }
    }

    private List<SeedNode> buildSeedTree() {
        return List.of(
                node("Web开发",
                        node("前端基础",
                                leaf("HTML"),
                                leaf("CSS"),
                                leaf("JavaScript")
                        ),
                        node("后端与部署",
                                leaf("MySQL建模"),
                                leaf("Nginx反向代理"))),
                node("数据结构与算法",
                        node("线性结构",
                                leaf("数组与链表"),
                                leaf("栈与队列")),
                        node("非线性结构",
                                leaf("二叉树遍历"),
                                leaf("图遍历与最短路径")),
                        node("算法思想",
                                leaf("递归与分治"),
                                leaf("动态规划"))),
                node("Java语法",
                        node("语言基础",
                                leaf("面向对象核心概念"),
                                leaf("异常处理")),
                        node("核心技能",
                                leaf("集合框架"),
                                leaf("线程池与锁"))),
                node("操作系统",
                        node("进程与线程",
                                leaf("进程线程模型"),
                                leaf("死锁与预防")),
                        node("内存与文件",
                                leaf("分页与分段"),
                                leaf("文件系统"))),
                node("计算机网络",
                        node("网络分层",
                                leaf("OSI与TCP/IP"),
                                leaf("IP与路由")),
                        node("传输层与应用层",
                                leaf("TCP拥塞控制"),
                                leaf("HTTP与HTTPS"))),
                node("人工智能导论",
                        node("AI基础",
                                leaf("机器学习基本概念"),
                                leaf("监督学习与无监督学习")),
                        node("深度学习入门",
                                leaf("神经网络基础"),
                                leaf("反向传播")),
                        node("AI工程",
                                leaf("数据预处理"),
                                leaf("模型部署")))
        );
    }

    private SeedNode node(String name, SeedNode... children) {
        SeedNode n = new SeedNode(name);
        n.children.addAll(Arrays.asList(children));
        return n;
    }

    private SeedNode leaf(String name) {
        return new SeedNode(name);
    }

    private String toJsonArray(List<TreeNode> nodes) {
        StringBuilder sb = new StringBuilder("[");
        Iterator<TreeNode> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            sb.append(toJsonObject(iterator.next()));
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJsonObject(TreeNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"id\":").append(node.id).append(",\"name\":\"").append(escape(node.name)).append("\"");
        if (!node.children.isEmpty()) {
            sb.append(",\"children\":").append(toJsonArray(node.children));
        }
        sb.append("}");
        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? null : parentId;
    }

    private int defaultLevel(Integer level) {
        return level == null || level <= 0 ? 1 : level;
    }

    private int nextSortNo(Long courseId) {
        KnowledgeEntity last = lambdaQuery()
                .eq(KnowledgeEntity::getCourseId, courseId)
                .orderByDesc(KnowledgeEntity::getSortNo)
                .orderByDesc(KnowledgeEntity::getId)
                .last("limit 1")
                .one();
        return last == null || last.getSortNo() == null ? 1 : last.getSortNo() + 1;
    }

    private String buildPath(String parentPath, Long currentId) {
        return !StringUtils.hasText(parentPath) ? String.valueOf(currentId) : parentPath + "/" + currentId;
    }

    private static class TreeNode {
        private final Long id;
        private final String name;
        private final List<TreeNode> children = new ArrayList<>();

        private TreeNode(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static class SeedNode {
        private final String name;
        private final List<SeedNode> children = new ArrayList<>();

        private SeedNode(String name) {
            this.name = name;
        }
    }
}
