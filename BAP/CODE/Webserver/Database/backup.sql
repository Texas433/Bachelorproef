BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "devices" (
	"device_id"	INTEGER,
	"mac_address"	TEXT NOT NULL UNIQUE,
	"user_id"	INTEGER NOT NULL,
	"is_allowed"	BOOLEAN DEFAULT TRUE,
	"created_at"	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	FOREIGN KEY("user_id") REFERENCES "users"("user_id"),
	PRIMARY KEY("device_id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "users" (
	"user_id"	INTEGER,
	"username"	TEXT NOT NULL UNIQUE,
	"password"	TEXT NOT NULL,
	"Token"	INTEGER UNIQUE,
	PRIMARY KEY("user_id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "sensor_data" (
	"data_id"	INTEGER,
	"user_id"	INTEGER NOT NULL,
	"temperature"	REAL,
	"humidity"	REAL,
	"pressure"	REAL,
	"timestamp"	DATETIME DEFAULT CURRENT_TIMESTAMP,
	"device_id"	INTEGER NOT NULL,
	FOREIGN KEY("user_id") REFERENCES "users"("user_id"),
	FOREIGN KEY("device_id") REFERENCES "devices"("device_id"),
	PRIMARY KEY("data_id" AUTOINCREMENT)
);
INSERT INTO "users" ("user_id","username","password","Token") VALUES (1,'admin','admin','');
INSERT INTO "sensor_data" ("data_id","user_id","temperature","humidity","pressure","timestamp","device_id") VALUES (1,1,0.0,0.0,0.0,'2025-02-14 15:25:18',0),
 (2,1,24.5,45.7,1013.2,'2025-02-14 15:33:25',12),
 (3,1,28.5,45.3,1013.1,'2025-02-14 16:29:02',20);
COMMIT;
