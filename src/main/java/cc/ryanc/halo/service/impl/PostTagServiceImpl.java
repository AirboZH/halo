package cc.ryanc.halo.service.impl;

import cc.ryanc.halo.model.dto.TagWithCountOutputDTO;
import cc.ryanc.halo.model.entity.Post;
import cc.ryanc.halo.model.entity.PostTag;
import cc.ryanc.halo.model.entity.Tag;
import cc.ryanc.halo.repository.PostRepository;
import cc.ryanc.halo.repository.PostTagRepository;
import cc.ryanc.halo.repository.TagRepository;
import cc.ryanc.halo.service.PostTagService;
import cc.ryanc.halo.service.base.AbstractCrudService;
import cc.ryanc.halo.utils.ServiceUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Post tag service implementation.
 *
 * @author johnniang
 * @date 3/19/19
 */
@Service
public class PostTagServiceImpl extends AbstractCrudService<PostTag, Integer> implements PostTagService {

    private final PostTagRepository postTagRepository;

    private final PostRepository postRepository;

    private final TagRepository tagRepository;

    public PostTagServiceImpl(PostTagRepository postTagRepository,
                              PostRepository postRepository,
                              TagRepository tagRepository) {
        super(postTagRepository);
        this.postTagRepository = postTagRepository;
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
    }

    @Override
    public List<Tag> listTagsBy(Integer postId) {
        Assert.notNull(postId, "Post id must not be null");

        // Find all tag ids
        Set<Integer> tagIds = postTagRepository.findAllTagIdsByPostId(postId);

        return tagRepository.findAllById(tagIds);
    }

    @Override
    public List<TagWithCountOutputDTO> listTagWithCountDtos(Sort sort) {
        Assert.notNull(sort, "Sort info must not be null");

        // Find all tags
        List<Tag> tags = tagRepository.findAll(sort);

        // Find post count
        return tags.stream().map(
                tag -> new TagWithCountOutputDTO().<TagWithCountOutputDTO>convertFrom(tag)
        ).collect(Collectors.toList());
    }

    @Override
    public Map<Integer, List<Tag>> listTagListMapBy(Collection<Integer> postIds) {
        if (CollectionUtils.isEmpty(postIds)) {
            return Collections.emptyMap();
        }

        // Find all post tags
        List<PostTag> postTags = postTagRepository.findAllByPostIdIn(postIds);

        // Fetch tag ids
        Set<Integer> tagIds = ServiceUtils.fetchProperty(postTags, PostTag::getTagId);

        // Find all tags
        List<Tag> tags = tagRepository.findAllById(tagIds);

        // Convert to tag map
        Map<Integer, Tag> tagMap = ServiceUtils.convertToMap(tags, Tag::getId);

        // Create tag list map
        Map<Integer, List<Tag>> tagListMap = new HashMap<>();

        // Foreach and collect
        postTags.forEach(postTag -> tagListMap.computeIfAbsent(postTag.getPostId(), postId -> new LinkedList<>()).add(tagMap.get(postTag.getTagId())));

        return tagListMap;
    }


    @Override
    public List<Post> listPostsBy(Integer tagId) {
        Assert.notNull(tagId, "Tag id must not be null");

        // Find all post ids
        Set<Integer> postIds = postTagRepository.findAllPostIdsByTagId(tagId);

        return postRepository.findAllById(postIds);
    }

    @Override
    public List<PostTag> mergeOrCreateByIfAbsent(Integer postId, Set<Integer> tagIds) {
        Assert.notNull(postId, "Post id must not be null");

        if (CollectionUtils.isEmpty(tagIds)) {
            return Collections.emptyList();
        }

        // Create post tags
        List<PostTag> postTagsStaging = tagIds.stream().map(tagId -> {
            // Build post tag
            PostTag postTag = new PostTag();
            postTag.setPostId(postId);
            postTag.setTagId(tagId);
            return postTag;
        }).collect(Collectors.toList());

        List<PostTag> postTagsToRemove = new LinkedList<>();
        List<PostTag> postTagsToCreate = new LinkedList<>();

        List<PostTag> postTags = postTagRepository.findAllByPostId(postId);

        postTags.forEach(postTag -> {
            if (!postTagsStaging.contains(postTag)) {
                postTagsToRemove.add(postTag);
            }
        });

        postTagsStaging.forEach(postTagStaging -> {
            if (!postTags.contains(postTagStaging)) {
                postTagsToCreate.add(postTagStaging);
            }
        });

        // Remove post tags
        removeAll(postTagsToRemove);

        // Remove all post tags need to remove
        postTags.removeAll(postTagsToRemove);

        // Add all created post tags
        postTags.addAll(createInBatch(postTagsToCreate));

        // Return post tags
        return postTags;
    }

    @Override
    public List<PostTag> listByPostId(Integer postId) {
        Assert.notNull(postId, "Post id must not be null");

        return postTagRepository.findAllByPostId(postId);
    }

    @Override
    public List<PostTag> listByTagId(Integer tagId) {
        Assert.notNull(tagId, "Tag id must not be null");

        return postTagRepository.findAllByTagId(tagId);
    }

    @Override
    public Set<Integer> listTagIdsByPostId(Integer postId) {
        Assert.notNull(postId, "Post id must not be null");

        return postTagRepository.findAllTagIdsByPostId(postId);
    }

    @Override
    public List<PostTag> removeByPostId(Integer postId) {
        Assert.notNull(postId, "Post id must not be null");

        return postTagRepository.deleteByPostId(postId);
    }

    @Override
    public List<PostTag> removeByTagId(Integer tagId) {
        Assert.notNull(tagId, "Tag id must not be null");

        return postTagRepository.deleteByTagId(tagId);
    }
}