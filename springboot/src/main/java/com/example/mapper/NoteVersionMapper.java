package com.example.mapper;

import com.example.entity.NoteVersion;
import org.apache.ibatis.annotations.*;

/**
 * 笔记版本数据访问层
 */
@Mapper
public interface NoteVersionMapper {

    @Insert("INSERT INTO note_version(note_id, version_number, title, content, change_summary, word_count) " +
            "VALUES(#{noteId}, #{versionNumber}, #{title}, #{content}, #{changeSummary}, #{wordCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NoteVersion version);

    @Select("SELECT MAX(version_number) FROM note_version WHERE note_id = #{noteId}")
    Integer selectMaxVersionNumber(@Param("noteId") Long noteId);
}