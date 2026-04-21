package com.example.mapper;

import com.example.entity.Note;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 笔记数据访问层
 */
@Mapper
public interface NoteMapper {

    /**
     * 新增笔记
     */
    @Insert("INSERT INTO note(user_id, title, content, content_type, category_id, folder_path, " +
            "is_favorite, is_pinned, is_public, word_count, status, version) " +
            "VALUES (#{userId}, #{title}, #{content}, #{contentType}, #{categoryId}, #{folderPath}, " +
            "#{isFavorite}, #{isPinned}, #{isPublic}, #{wordCount}, #{status}, #{version})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Note note);

    /**
     * 删除笔记
     */
    @Delete("DELETE FROM note WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 逻辑删除笔记
     */
    @Update("UPDATE note SET status = -1 WHERE id = #{id}")
    int logicDeleteById(Long id);

    /**
     * 修改笔记
     */
    @Update("UPDATE note SET title = #{title}, content = #{content}, content_type = #{contentType}, " +
            "category_id = #{categoryId}, folder_path = #{folderPath}, is_favorite = #{isFavorite}, " +
            "is_pinned = #{isPinned}, is_public = #{isPublic}, word_count = #{wordCount}, " +
            "status = #{status}, version = #{version} WHERE id = #{id}")
    int updateById(Note note);

    /**
     * 根据ID查询笔记
     */
    @Select("SELECT * FROM note WHERE id = #{id} AND status != -1")
    Note selectById(Long id);

    /**
     * 根据用户ID查询所有笔记
     */
    @Select("SELECT * FROM note WHERE user_id = #{userId} AND status != -1 ORDER BY updated_time DESC")
    List<Note> selectByUserId(Integer userId);

    /**
     * 根据用户ID和状态查询笔记
     */
    @Select("SELECT * FROM note WHERE user_id = #{userId} AND status = #{status} ORDER BY updated_time DESC")
    List<Note> selectByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") Integer status);

    /**
     * 根据分类ID查询笔记
     */
    @Select("SELECT * FROM note WHERE category_id = #{categoryId} AND status != -1 ORDER BY updated_time DESC")
    List<Note> selectByCategoryId(Integer categoryId);

    /**
     * 查询用户收藏的笔记
     */
    @Select("SELECT * FROM note WHERE user_id = #{userId} AND is_favorite = 1 AND status != -1 ORDER BY updated_time DESC")
    List<Note> selectFavoritesByUserId(Integer userId);

    /**
     * 查询用户置顶的笔记
     */
    @Select("SELECT * FROM note WHERE user_id = #{userId} AND is_pinned = 1 AND status != -1 ORDER BY updated_time DESC")
    List<Note> selectPinnedByUserId(Integer userId);

    /**
     * 根据标题搜索笔记
     */
    @Select("SELECT * FROM note WHERE user_id = #{userId} AND title LIKE CONCAT('%', #{keyword}, '%') " +
            "AND status != -1 ORDER BY updated_time DESC")
    List<Note> searchByTitle(@Param("userId") Integer userId, @Param("keyword") String keyword);

    /**
     * 全文搜索笔记（标题和内容）
     */
    @Select("SELECT * FROM note WHERE user_id = #{userId} AND " +
            "(title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND status != -1 ORDER BY updated_time DESC")
    List<Note> searchByContent(@Param("userId") Integer userId, @Param("keyword") String keyword);

    /**
     * 更新笔记查看次数
     */
    @Update("UPDATE note SET view_count = view_count + 1, last_viewed_time = NOW() WHERE id = #{id}")
    int incrementViewCount(Long id);

    /**
     * 切换笔记收藏状态
     */
    @Update("UPDATE note SET is_favorite = #{isFavorite} WHERE id = #{id}")
    int updateFavoriteStatus(@Param("id") Long id, @Param("isFavorite") Boolean isFavorite);

    /**
     * 切换笔记置顶状态
     */
    @Update("UPDATE note SET is_pinned = #{isPinned} WHERE id = #{id}")
    int updatePinnedStatus(@Param("id") Long id, @Param("isPinned") Boolean isPinned);

    /**
     * 切换笔记公开状态
     */
    @Update("UPDATE note SET is_public = #{isPublic} WHERE id = #{id}")
    int updatePublicStatus(@Param("id") Long id, @Param("isPublic") Boolean isPublic);

    /**
     * 统计用户笔记数量
     */
    @Select("SELECT COUNT(*) FROM note WHERE user_id = #{userId} AND status != -1")
    int countByUserId(Integer userId);

    /**
     * 统计用户各状态笔记数量
     */
    @Select("SELECT COUNT(*) FROM note WHERE user_id = #{userId} AND status = #{status}")
    int countByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") Integer status);

    /**
     * 分页查询用户笔记
     */
    @Select("SELECT * FROM note WHERE user_id = #{userId} AND status != -1 " +
            "ORDER BY updated_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<Note> selectByUserIdWithPage(@Param("userId") Integer userId, 
                                      @Param("limit") Integer limit, 
                                      @Param("offset") Integer offset);

    /**
     * 查询最近更新的笔记
     */
    @Select("SELECT * FROM note WHERE user_id = #{userId} AND status != -1 " +
            "ORDER BY updated_time DESC LIMIT #{limit}")
    List<Note> selectRecentByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit);

    /**
     * 根据用户ID、标签查询笔记
     */
    @Select("SELECT n.* FROM note n INNER JOIN note_tag nt ON n.id = nt.note_id " +
            "WHERE n.user_id = #{userId} AND nt.tag_id = #{tagId} AND n.status != -1 " +
            "ORDER BY n.updated_time DESC")
    List<Note> selectByUserIdAndTagId(@Param("userId") Integer userId, @Param("tagId") Integer tagId);

    /**
     * 根据用户ID、状态和标签查询笔记
     */
    @Select("SELECT n.* FROM note n INNER JOIN note_tag nt ON n.id = nt.note_id " +
            "WHERE n.user_id = #{userId} AND n.status = #{status} AND nt.tag_id = #{tagId} " +
            "ORDER BY n.updated_time DESC")
    List<Note> selectByUserIdAndStatusAndTagId(@Param("userId") Integer userId,
                                               @Param("status") Integer status,
                                               @Param("tagId") Integer tagId);
}