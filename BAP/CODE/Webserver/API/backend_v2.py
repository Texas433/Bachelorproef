from flask import Flask, request, jsonify
import sqlite3
import jwt
import datetime
import secrets
from Cryptodome.Cipher import AES
from Cryptodome.Util.Padding import unpad

app = Flask(__name__)
app.config['SECRET_KEY'] = secrets.token_hex(32)

DATABASE = 'Database/BLE_storage'

KEY = b'1234567890ABCDEF'  # Moet overeenkomen met de ESP32
HEADER_SIZE = 24
IV_SIZE = 16
HASH_SIZE = 32

def get_db_connection():
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    return conn

def decrypt_data(encrypted_hex_string):
    # 1. Verwerk "32 30 7C ..." naar bytes
    hex_parts = encrypted_hex_string.strip().split()
    encrypted_bytes = bytes([int(h, 16) for h in hex_parts])

    # 2. Check structuur
    if len(encrypted_bytes) < HEADER_SIZE + IV_SIZE + HASH_SIZE:
        raise ValueError("Packet too short")

    # 3. Extract onderdelen
    header = encrypted_bytes[:HEADER_SIZE]
    if not header.startswith(b'HEADERFIXED'):
        raise ValueError("Invalid header")

    iv = encrypted_bytes[HEADER_SIZE:HEADER_SIZE + IV_SIZE]
    cipher_text_end = len(encrypted_bytes) - HASH_SIZE
    cipher_text = encrypted_bytes[HEADER_SIZE + IV_SIZE:cipher_text_end]

    # 4. AES decryptie
    cipher = AES.new(KEY, AES.MODE_CBC, iv)
    padded = cipher.decrypt(cipher_text)
    plain = unpad(padded, AES.block_size)

    return plain.decode('utf-8')

def parse_sensor_data(decrypted_data):
    parts = decrypted_data.split('|')
    device_id = int(parts[0])
    temperature = float(parts[1].replace('C', ''))
    pressure = float(parts[2].replace('hPA', ''))
    humidity = float(parts[3].replace('%', ''))
    return device_id, temperature, pressure, humidity

@app.route('/')
def index():
    return "Bachelorproef API is running!"

@app.route('/login', methods=['POST'])
def login():
    data = request.json
    username = data.get('username')
    password = data.get('password')
    conn = get_db_connection()
    user = conn.execute('SELECT * FROM users WHERE username = ? AND password = ?', (username, password)).fetchone()
    conn.close()
    if user:
        token = jwt.encode({'user_id': user['user_id'], 'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=1)}, app.config['SECRET_KEY'], algorithm='HS256')
        return jsonify({"token": token, "user_id": user['user_id']})
    return jsonify({"error": "Invalid credentials"}), 401

@app.route('/upload', methods=['POST'])
def upload():
    token = request.headers.get('Authorization')
    if not token:
        return jsonify({'error': 'Token is missing!'}), 403
    try:
        data = jwt.decode(token.split()[1], app.config['SECRET_KEY'], algorithms=['HS256'])
    except:
        return jsonify({'error': 'Token is invalid!'}), 403

    user_id = data['user_id']
    encrypted_hex = request.json.get('data')

    try:
        decrypted_string = decrypt_data(encrypted_hex)
        print("[DEBUG] Decrypted string:", decrypted_string)
        device_id, temperature, pressure, humidity = parse_sensor_data(decrypted_string)
    except Exception as e:
        return jsonify({"error": f"Decryption failed: {str(e)}"}), 400

    conn = get_db_connection()
    conn.execute(
        'INSERT INTO sensor_data (user_id, device_id, temperature, humidity, pressure) VALUES (?, ?, ?, ?, ?)',
        (user_id, device_id, temperature, humidity, pressure))
    conn.commit()
    conn.close()
    return jsonify({"message": "Data uploaded successfully"})

@app.route('/data', methods=['GET'])
def fetch_data():
    token = request.headers.get('Authorization')
    if not token:
        return jsonify({'error': 'Token is missing!'}), 403
    try:
        token_data = jwt.decode(token.split()[1], app.config['SECRET_KEY'], algorithms=['HS256'])
    except Exception as e:
        return jsonify({'error': 'Token is invalid!'}), 403

    user_id = token_data['user_id']
    conn = get_db_connection()
    data = conn.execute('SELECT * FROM sensor_data WHERE user_id = ? ORDER BY timestamp DESC', (user_id,)).fetchall()
    conn.close()

    result = [dict(row) for row in data]
    return jsonify(result)

@app.route('/fetch', methods=['GET'])
def fetch_latest():
    token = request.headers.get('Authorization')
    if not token:
        return jsonify({'error': 'Token is missing!'}), 403
    try:
        data = jwt.decode(token.split()[1], app.config['SECRET_KEY'], algorithms=['HS256'])
    except:
        return jsonify({'error': 'Token is invalid!'}), 403

    user_id = data['user_id']
    conn = get_db_connection()
    latest = conn.execute('SELECT * FROM sensor_data WHERE user_id = ? ORDER BY timestamp DESC LIMIT 1', (user_id,)).fetchone()
    conn.close()

    if latest:
        return jsonify(dict(latest))
    return jsonify({'error': 'No data found'})

if __name__ == '__main__':
    app.run(debug=True, port=5000)
