BEGIN TRANSACTION;

CREATE TABLE IF NOT EXISTS "devices" (
	"device_id"	INTEGER PRIMARY KEY AUTOINCREMENT,
	"mac_address"	TEXT NOT NULL UNIQUE,
	"user_id"	INTEGER NOT NULL,
	"is_allowed"	BOOLEAN DEFAULT TRUE,
	"created_at"	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	FOREIGN KEY("user_id") REFERENCES "users"("user_id")
);

CREATE TABLE IF NOT EXISTS "users" (
	"user_id"	INTEGER PRIMARY KEY AUTOINCREMENT,
	"username"	TEXT NOT NULL UNIQUE,
	"password"	TEXT NOT NULL,
	"Token"	TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS "sensor_data" (
	"data_id"	INTEGER PRIMARY KEY AUTOINCREMENT,
	"user_id"	INTEGER NOT NULL,
	"temperature"	REAL,
	"humidity"	REAL,
	"pressure"	REAL,
	"timestamp"	DATETIME DEFAULT CURRENT_TIMESTAMP,
	"device_id"	INTEGER NOT NULL,
	FOREIGN KEY("user_id") REFERENCES "users"("user_id"),
	FOREIGN KEY("device_id") REFERENCES "devices"("device_id")
);

-- Voeg admin user toe
INSERT INTO "users" ("username","password","Token") VALUES ('admin','admin', NULL);

-- Voeg devices toe
INSERT INTO "devices" ("mac_address", "user_id") VALUES ('00:1A:7D:DA:71:13', 1);
INSERT INTO "devices" ("mac_address", "user_id") VALUES ('00:1A:7D:DA:71:14', 1);

-- Voeg sensor data toe
INSERT INTO "sensor_data" ("user_id","temperature","humidity","pressure","device_id") 
VALUES (1, 24.5, 45.7, 1013.2, 1), 
       (1, 28.5, 45.3, 1013.1, 2);

COMMIT;

