BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "users" (
	"user_id"	INTEGER,
	"username"	TEXT NOT NULL UNIQUE,
	"password"	TEXT NOT NULL,
	PRIMARY KEY("user_id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "sensor_data" (
	"data_id"	INTEGER,
	"user_id"	INTEGER NOT NULL,
	"temperature"	REAL,
	"humidity"	REAL,
	"pressure"	REAL,
	"timestamp"	DATETIME DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY("data_id" AUTOINCREMENT),
	FOREIGN KEY("user_id") REFERENCES "users"("user_id")
);
CREATE TABLE IF NOT EXISTS "devices" (
	"device_id"	INTEGER,
	"mac_address"	TEXT NOT NULL UNIQUE,
	"user_id"	INTEGER NOT NULL,
	"is_allowed"	BOOLEAN DEFAULT TRUE,
	"created_at"	TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	FOREIGN KEY("user_id") REFERENCES "users"("user_id"),
	PRIMARY KEY("device_id" AUTOINCREMENT)
);
COMMIT;
