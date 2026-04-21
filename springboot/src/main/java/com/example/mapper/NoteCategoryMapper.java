package com.example.mapper;

import com.example.entity.NoteCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 笔记分类数据访问层
 */
@Mapper
public interface NoteCategoryMapper {

    /**
     * 新增分类
     */
    @Insert("INSERT INTO note_category(user_id, name, description, color, icon, parent_id, sort_order) " +
            "VALUES (#{userId}, #{name}, #{description}, #{color}, #{icon}, #{parentId}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NoteCategory category);

    /**
     * 删除分类
     */
    @Delete("DELETE FROM note_category WHERE id = #{id}")
    int deleteById(Integer id);

    /**
     * 修改分类
     */
    @Update("UPDATE note_category SET name = #{name}, description = #{description}, " +
            "color = #{color}, icon = #{icon}, parent_id = #{parentId}, sort_order = #{sortOrder} " +
            "WHERE id = #{id}")
    int updateById(NoteCategory category);

    /**
     * 根据ID查询分类
     */
    @Select("SELECT * FROM note_category WHERE id = #{id}")
    NoteCategory selectById(Integer id);

    /**
     * 根据用户ID查询所有分类
     */
    @Select("SELECT * FROM note_category WHERE user_id = #{userId} ORDER BY sort_order ASC, created_time ASC")
    List<NoteCategory> selectByUserId(Integer userId);

    /**
     * 根据父分类ID查询子分类
     */
    @Select("SELECT * FROM note_category WHERE parent_id = #{parentId} ORDER BY sort_order ASC, created_time ASC")
    List<NoteCategory> selectByParentId(Integer parentId);

    /**
     * 查询根分类（无父分类）
     */
    @Select("SELECT * FROM note_category WHERE user_id = #{userId} AND parent_id IS NULL " +
            "ORDER BY sort_order ASC, created_time ASC")
    List<NoteCategory> selectRootByUserId(Integer userId);

    /**
     * 根据名称查询分类
     */
    @Select("SELECT * FROM note_category WHERE user_id = #{userId} AND name = #{name}")
    NoteCategory selectByUserIdAndName(@Param("userId") Integer userId, @Param("name") String name);

    /**
     * 更新分类笔记数量
     */
    @Update("UPDATE note_category SET note_count = #{noteCount} WHERE id = #{id}")
    int updateNoteCount(@Param("id") Integer id, @Param("noteCount") Integer noteCount);

    /**
     * 统计分类下的笔记数量
     */
    @Select("SELECT COUNT(*) FROM note WHERE category_id = #{categoryId} AND status != -1")
    int countNotesByCategoryId(Integer categoryId);

    /**
     * 检查分类是否有子分类
     */
    @Select("SELECT COUNT(*) FROM note_category WHERE parent_id = #{categoryId}")
    int countChildrenByCategoryId(Integer categoryId);

    /**
     * 获取用户分类的最大排序号
     */
    @Select("SELECT COALESCE(MAX(sort_order), 0) FROM note_category WHERE user_id = #{userId}")
    int getMaxSortOrderByUserId(Integer userId);

    /**
     * 批量更新排序
     */
    @Update("UPDATE note_category SET sort_order = #{sortOrder} WHERE id = #{id}")
    int updateSortOrder(@Param("id") Integer id, @Param("sortOrder") Integer sortOrder);
}