package com.example.mapper;

import com.example.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 标签数据访问层
 */
@Mapper
public interface TagMapper {

    /**
     * 新增标签
     */
    @Insert("INSERT INTO tag(user_id, name, color, description) " +
            "VALUES (#{userId}, #{name}, #{color}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tag tag);

    /**
     * 删除标签
     */
    @Delete("DELETE FROM tag WHERE id = #{id}")
    int deleteById(Integer id);

    /**
     * 修改标签
     */
    @Update("UPDATE tag SET name = #{name}, color = #{color}, description = #{description} WHERE id = #{id}")
    int updateById(Tag tag);

    /**
     * 根据ID查询标签
     */
    @Select("SELECT * FROM tag WHERE id = #{id}")
    Tag selectById(Integer id);

    /**
     * 根据用户ID查询所有标签
     */
    @Select("SELECT * FROM tag WHERE user_id = #{userId} ORDER BY use_count DESC, created_time DESC")
    List<Tag> selectByUserId(Integer userId);

    /**
     * 根据用户ID和标签名查询标签
     */
    @Select("SELECT * FROM tag WHERE user_id = #{userId} AND name = #{name}")
    Tag selectByUserIdAndName(@Param("userId") Integer userId, @Param("name") String name);

    /**
     * 根据笔记ID查询关联的标签
     */
    @Select("SELECT t.* FROM tag t INNER JOIN note_tag nt ON t.id = nt.tag_id WHERE nt.note_id = #{noteId}")
    List<Tag> selectByNoteId(Long noteId);

    /**
     * 搜索标签（按名称模糊匹配）
     */
    @Select("SELECT * FROM tag WHERE user_id = #{userId} AND name LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY use_count DESC, created_time DESC")
    List<Tag> searchByName(@Param("userId") Integer userId, @Param("keyword") String keyword);

    /**
     * 获取热门标签（按使用次数排序）
     */
    @Select("SELECT * FROM tag WHERE user_id = #{userId} AND use_count > 0 " +
            "ORDER BY use_count DESC LIMIT #{limit}")
    List<Tag> selectPopularByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit);

    /**
     * 更新标签使用次数
     */
    @Update("UPDATE tag SET use_count = #{useCount} WHERE id = #{id}")
    int updateUseCount(@Param("id") Integer id, @Param("useCount") Integer useCount);

    /**
     * 增加标签使用次数
     */
    @Update("UPDATE tag SET use_count = use_count + 1 WHERE id = #{id}")
    int incrementUseCount(Integer id);

    /**
     * 减少标签使用次数
     */
    @Update("UPDATE tag SET use_count = GREATEST(use_count - 1, 0) WHERE id = #{id}")
    int decrementUseCount(Integer id);

    /**
     * 统计用户标签数量
     */
    @Select("SELECT COUNT(*) FROM tag WHERE user_id = #{userId}")
    int countByUserId(Integer userId);

    /**
     * 批量插入标签
     */
    @Insert("<script>" +
            "INSERT INTO tag(user_id, name, color, description) VALUES " +
            "<foreach collection='tags' item='tag' separator=','>" +
            "(#{tag.userId}, #{tag.name}, #{tag.color}, #{tag.description})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("tags") List<Tag> tags);

    /**
     * 查询未使用的标签
     */
    @Select("SELECT * FROM tag WHERE user_id = #{userId} AND use_count = 0 ORDER BY created_time DESC")
    List<Tag> selectUnusedByUserId(Integer userId);
}