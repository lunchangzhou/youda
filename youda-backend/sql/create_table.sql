-- auto-generated definition
create table user
(
    id           bigint auto_increment comment '主键'
        primary key,
    username     varchar(256)                       null comment '昵称',
    userAccount  varchar(256)                       null comment '登录账号',
    avatarUrl    varchar(1024)                      null comment '头像',
    gender       tinyint                            null comment '性别 0 男 1 女',
    userPassword varchar(512)                       not null comment '密码',
    phone        varchar(128)                       null comment '电话',
    email        varchar(512)                       null comment '邮箱',
    userStatus   int      default 0                 not null comment ' 用户状态 0 正常 1 ...',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除 0 1（逻辑删除）',
    userRole     int      default 0                 not null comment '用户角色 0 普通用户 1 管理员',
    planetCode   varchar(255)                       null comment '星球编号'
);

-- auto-generated definition
create table tag
(
    id         bigint auto_increment comment '主键'
        primary key,
    tagName    varchar(256)                       null comment '标签名',
    userId     int                                null comment '用户Id',
    parentId   int                                null comment '父标签Id',
    isParent   tinyint                            null comment '是否为父标签 0 不是 1 是',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除 0 1（逻辑删除）'
);

