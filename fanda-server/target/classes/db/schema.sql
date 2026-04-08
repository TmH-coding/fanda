CREATE DATABASE IF NOT EXISTS fanda DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fanda;

-- 用户表
CREATE TABLE IF NOT EXISTS fd_user (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,
  nickname    VARCHAR(50)  NOT NULL DEFAULT '',
  avatar      VARCHAR(20)  NOT NULL DEFAULT '',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_username (username)
) ENGINE=InnoDB;

-- 菜品主表
CREATE TABLE IF NOT EXISTS fd_food_item (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  food_code     VARCHAR(20)  NOT NULL UNIQUE,
  name          VARCHAR(100) NOT NULL,
  category      VARCHAR(30)  NOT NULL,
  sub_category  VARCHAR(50)  NOT NULL DEFAULT '',
  price_min     DECIMAL(8,2) NOT NULL DEFAULT 0,
  price_max     DECIMAL(8,2) NOT NULL DEFAULT 0,
  is_system     TINYINT(1)   NOT NULL DEFAULT 1,
  user_id       BIGINT       NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_category (category),
  INDEX idx_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_food_tag (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  food_id  BIGINT      NOT NULL,
  tag      VARCHAR(30) NOT NULL,
  FOREIGN KEY (food_id) REFERENCES fd_food_item(id) ON DELETE CASCADE,
  INDEX idx_food (food_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_food_nutrition (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  food_id         BIGINT      NOT NULL,
  nutrition_type  VARCHAR(20) NOT NULL,
  FOREIGN KEY (food_id) REFERENCES fd_food_item(id) ON DELETE CASCADE,
  INDEX idx_food (food_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_food_allergen (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  food_id  BIGINT      NOT NULL,
  allergen VARCHAR(30) NOT NULL,
  FOREIGN KEY (food_id) REFERENCES fd_food_item(id) ON DELETE CASCADE,
  INDEX idx_food (food_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_food_meal_time (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  food_id   BIGINT      NOT NULL,
  meal_time VARCHAR(20) NOT NULL,
  FOREIGN KEY (food_id) REFERENCES fd_food_item(id) ON DELETE CASCADE,
  INDEX idx_food (food_id)
) ENGINE=InnoDB;

-- 用餐记录
CREATE TABLE IF NOT EXISTS fd_meal_record (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT       NOT NULL,
  record_date DATE         NOT NULL,
  meal_type   VARCHAR(20)  NOT NULL,
  food_name   VARCHAR(100) NOT NULL,
  food_id     BIGINT       NULL,
  cost        DECIMAL(8,2) NOT NULL DEFAULT 0,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES fd_user(id) ON DELETE CASCADE,
  INDEX idx_user_date (user_id, record_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_record_nutrition (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  record_id       BIGINT      NOT NULL,
  nutrition_type  VARCHAR(20) NOT NULL,
  FOREIGN KEY (record_id) REFERENCES fd_meal_record(id) ON DELETE CASCADE,
  INDEX idx_record (record_id)
) ENGINE=InnoDB;

-- 预算
CREATE TABLE IF NOT EXISTS fd_budget (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT        NOT NULL UNIQUE,
  monthly    DECIMAL(10,2) NOT NULL DEFAULT 2000,
  weekly     DECIMAL(10,2) NOT NULL DEFAULT 500,
  updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES fd_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 支出
CREATE TABLE IF NOT EXISTS fd_expense (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT       NOT NULL,
  expense_date DATE         NOT NULL,
  amount       DECIMAL(8,2) NOT NULL,
  meal_type    VARCHAR(20)  NOT NULL,
  description  VARCHAR(200) NOT NULL DEFAULT '',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES fd_user(id) ON DELETE CASCADE,
  INDEX idx_user_date (user_id, expense_date)
) ENGINE=InnoDB;

-- 用户偏好
CREATE TABLE IF NOT EXISTS fd_user_preference (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT   NOT NULL UNIQUE,
  spicy_level TINYINT  NOT NULL DEFAULT 2,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES fd_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_user_allergy (
  id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT      NOT NULL,
  allergy VARCHAR(30) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES fd_user(id) ON DELETE CASCADE,
  INDEX idx_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_user_fav_category (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id  BIGINT      NOT NULL,
  category VARCHAR(30) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES fd_user(id) ON DELETE CASCADE,
  INDEX idx_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_user_dislike (
  id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT      NOT NULL,
  dislike VARCHAR(30) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES fd_user(id) ON DELETE CASCADE,
  INDEX idx_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_user_favorite_food (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT   NOT NULL,
  food_id    BIGINT   NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES fd_user(id) ON DELETE CASCADE,
  FOREIGN KEY (food_id) REFERENCES fd_food_item(id) ON DELETE CASCADE,
  UNIQUE INDEX idx_user_food (user_id, food_id)
) ENGINE=InnoDB;

-- 拼饭
CREATE TABLE IF NOT EXISTS fd_social_group (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  creator_id     BIGINT       NOT NULL,
  creator_name   VARCHAR(50)  NOT NULL,
  avatar         VARCHAR(20)  NOT NULL DEFAULT '',
  title          VARCHAR(200) NOT NULL,
  meal_time      VARCHAR(10)  NOT NULL,
  location       VARCHAR(200) NOT NULL,
  max_people     INT          NOT NULL DEFAULT 4,
  current_people INT          NOT NULL DEFAULT 1,
  status         VARCHAR(20)  NOT NULL DEFAULT 'open',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (creator_id) REFERENCES fd_user(id) ON DELETE CASCADE,
  INDEX idx_status (status),
  INDEX idx_created (created_at DESC)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_social_tag (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_id BIGINT      NOT NULL,
  tag      VARCHAR(30) NOT NULL,
  FOREIGN KEY (group_id) REFERENCES fd_social_group(id) ON DELETE CASCADE,
  INDEX idx_group (group_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_social_candidate (
  id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_id BIGINT       NOT NULL,
  name     VARCHAR(100) NOT NULL,
  votes    INT          NOT NULL DEFAULT 0,
  FOREIGN KEY (group_id) REFERENCES fd_social_group(id) ON DELETE CASCADE,
  INDEX idx_group (group_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_social_member (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_id  BIGINT   NOT NULL,
  user_id   BIGINT   NOT NULL,
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (group_id) REFERENCES fd_social_group(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES fd_user(id) ON DELETE CASCADE,
  UNIQUE INDEX idx_group_user (group_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS fd_social_vote (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_id     BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  candidate_id BIGINT NOT NULL,
  FOREIGN KEY (group_id) REFERENCES fd_social_group(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES fd_user(id) ON DELETE CASCADE,
  FOREIGN KEY (candidate_id) REFERENCES fd_social_candidate(id) ON DELETE CASCADE,
  UNIQUE INDEX idx_group_user (group_id, user_id)
) ENGINE=InnoDB;

-- 成就
CREATE TABLE IF NOT EXISTS fd_achievement_unlock (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT      NOT NULL,
  achievement_id VARCHAR(50) NOT NULL,
  unlocked_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES fd_user(id) ON DELETE CASCADE,
  UNIQUE INDEX idx_user_ach (user_id, achievement_id)
) ENGINE=InnoDB;
