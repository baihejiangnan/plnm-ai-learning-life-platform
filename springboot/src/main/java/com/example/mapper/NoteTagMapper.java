package com.example.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 笔记标签关联数据访问层
 */
@Mapper
public interface NoteTagMapper {

    /**
     * 添加笔记标签关联
     */
    @Insert("INSERT INTO note_tag(note_id, tag_id) VALUES (#{noteId}, #{tagId})")
    int insert(@Param("noteId") Long noteId, @Param("tagId") Integer tagId);

    /**
     * 删除笔记标签关联
     */
    @Delete("DELETE FROM note_tag WHERE note_id = #{noteId} AND tag_id = #{tagId}")
    int delete(@Param("noteId") Long noteId, @Param("tagId") Integer tagId);

    /**
     * 删除笔记的所有标签关联
     */
    @Delete("DELETE FROM note_tag WHERE note_id = #{noteId}")
    int deleteByNoteId(Long noteId);

    /**
     * 删除标签的所有笔记关联
     */
    @Delete("DELETE FROM note_tag WHERE tag_id = #{tagId}")
    int deleteByTagId(Integer tagId);

    /**
     * 查询笔记关联的标签ID列表
     */
    @Select("SELECT tag_id FROM note_tag WHERE note_id = #{noteId}")
    List<Integer> selectTagIdsByNoteId(Long noteId);

    /**
     * 查询标签关联的笔记ID列表
     */
    @Select("SELECT note_id FROM note_tag WHERE tag_id = #{tagId}")
    List<Long> selectNoteIdsByTagId(Integer tagId);

    /**
     * 检查笔记标签关联是否存在
     */
    @Select("SELECT COUNT(*) FROM note_tag WHERE note_id = #{noteId} AND tag_id = #{tagId}")
    int exists(@Param("noteId") Long noteId, @Param("tagId") Integer tagId);

    /**
     * 统计笔记的标签数量
     */
    @Select("SELECT COUNT(*) FROM note_tag WHERE note_id = #{noteId}")
    int countTagsByNoteId(Long noteId);

    /**
     * 统计标签的笔记数量
     */
    @Select("SELECT COUNT(*) FROM note_tag WHERE tag_id = #{tagId}")
    int countNotesByTagId(Integer tagId);

    /**
     * 批量添加笔记标签关联
     */
    @Insert("<script>" +
            "INSERT INTO note_tag(note_id, tag_id) VALUES " +
            "<foreach collection='tagIds' item='tagId' separator=','>" +
            "(#{noteId}, #{tagId})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("noteId") Long noteId, @Param("tagIds") List<Integer> tagIds);

    /**
     * 批量删除笔记标签关联
     */
    @Delete("<script>" +
            "DELETE FROM note_tag WHERE note_id = #{noteId} AND tag_id IN " +
            "<foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>" +
            "#{tagId}" +
            "</foreach>" +
            "</script>")
    int batchDelete(@Param("noteId") Long noteId, @Param("tagIds") List<Integer> tagIds);

    /**
     * 替换笔记的标签关联（先删除所有，再批量添加）
     */
    default int replaceNoteTags(Long noteId, List<Integer> tagIds) {
        deleteByNoteId(noteId);
        if (tagIds != null && !tagIds.isEmpty()) {
            return batchInsert(noteId, tagIds);
        }
        return 0;
    }

    /**
     * 查询共同标签的笔记（推荐相关笔记）
     */
    @Select("SELECT DISTINCT nt2.note_id FROM note_tag nt1 " +
            "INNER JOIN note_tag nt2 ON nt1.tag_id = nt2.tag_id " +
            "WHERE nt1.note_id = #{noteId} AND nt2.note_id != #{noteId} " +
            "LIMIT #{limit}")
    List<Long> selectRelatedNoteIds(@Param("noteId") Long noteId, @Param("limit") Integer limit);

    /**
     * 查询用户最常用的标签组合
     */
    @Select("SELECT nt1.tag_id as tag1, nt2.tag_id as tag2, COUNT(*) as count " +
            "FROM note_tag nt1 " +
            "INNER JOIN note_tag nt2 ON nt1.note_id = nt2.note_id AND nt1.tag_id < nt2.tag_id " +
            "INNER JOIN note n ON nt1.note_id = n.id " +
            "WHERE n.user_id = #{userId} " +
            "GROUP BY nt1.tag_id, nt2.tag_id " +
            "ORDER BY count DESC " +
            "LIMIT #{limit}")
    List<Object> selectPopularTagCombinations(@Param("userId") Integer userId, @Param("limit") Integer limit);
}