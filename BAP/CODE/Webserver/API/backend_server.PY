from flask import Flask, request, jsonify
import sqlite3
import jwt
import datetime
import secrets

app = Flask(__name__)
app.config['SECRET_KEY'] = secrets.token_hex(32)

DATABASE = 'Database/BLE_storage'

def get_db_connection():
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    return conn

def decrypt_data(encrypted_data):
    # Splits de hexadecimale waarden
    hex_values = encrypted_data.split()
    
    # Converteer hexadecimale waarden naar bytes en decodeer naar tekst
    decoded_text = ''.join(chr(int(h, 16)) for h in hex_values)
    
    return decoded_text


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
        return jsonify({"token": token,"user_id": user['user_id']})
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
    encrypted_data = request.json.get('data')
    decrypted_data = decrypt_data(encrypted_data)
    device_id, temperature, pressure, humidity = parse_sensor_data(decrypted_data or "0|0C|0hPA|0%") ##encrypted_data vervangen met decrypted_data manuel
    conn = get_db_connection()
    conn.execute('INSERT INTO sensor_data (user_id, device_id, temperature, humidity, pressure) VALUES (?, ?, ?, ?, ?)', (user_id, device_id, temperature, pressure, humidity))
    conn.commit()
    conn.close()
    return jsonify({"message": "Data uploaded successfully"})

@app.route('/data', methods=['GET'])
def fetch_data():
    token = request.headers.get('Authorization')
    print("Ontvangen Token:", token)  # Debugging

    if not token:
        return jsonify({'error': 'Token is missing!'}), 403

    try:
        token_data = jwt.decode(token.split()[1], app.config['SECRET_KEY'], algorithms=['HS256'])
        print("Gedecrypteerde Token:", token_data)  # Debugging
    except Exception as e:
        print("Token Error:", str(e))  # Debugging
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
    app.run(debug=True,port = 5000)
