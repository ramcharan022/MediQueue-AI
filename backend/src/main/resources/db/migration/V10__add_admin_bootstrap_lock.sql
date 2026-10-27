CREATE TABLE admin_bootstrap_lock (
    id TINYINT NOT NULL,
    CONSTRAINT pk_admin_bootstrap_lock PRIMARY KEY (id)
) ENGINE=InnoDB;

INSERT INTO admin_bootstrap_lock (id) VALUES (1);
