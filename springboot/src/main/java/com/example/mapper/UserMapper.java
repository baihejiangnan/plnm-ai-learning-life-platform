package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper {

    /**
     * 新增用户
     */
    @Insert("INSERT INTO user(username, password, name, avatar, role) VALUES (#{username}, #{password}, #{name}, #{avatar}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 删除用户
     */
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(Integer id);

    /**
     * 修改用户
     */
    @Update("UPDATE user SET username = #{username}, password = #{password}, name = #{name}, avatar = #{avatar}, role = #{role} WHERE id = #{id}")
    int updateById(User user);

    // 新增：仅更新用户密码
    @Update("UPDATE user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    // 新增：仅更新昵称和头像
    @Update("UPDATE user SET name = #{name}, avatar = #{avatar} WHERE id = #{id}")
    int updateProfile(@Param("id") Integer id, @Param("name") String name, @Param("avatar") String avatar);

    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(Integer id);

    /**
     * 查询所有用户
     */
    @Select("SELECT * FROM user")
    List<User> selectAll();

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(String username);

    /**
     * 根据用户名和密码查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND password = #{password}")
    User selectByUsernameAndPassword(@Param("username") String username, @Param("password") String password);
}