from flask import Flask, render_template, request, redirect, url_for, session, flash
import requests
import secrets
import logging

app = Flask(__name__)
app.secret_key = secrets.token_hex(32)  
API_URL = "http://127.0.0.1:5000"

# Configure logging
logging.basicConfig(level=logging.DEBUG)

def is_logged_in():
    return 'token' in session

def get_auth_headers():
    return {"Authorization": f"Bearer {session.get('token')}"} if is_logged_in() else {}

def handle_redirect():
    return redirect(url_for('login_page')) if not is_logged_in() else None

@app.route('/')
def home():
    return handle_redirect() or render_template('home.html')

@app.route('/login', methods=['GET', 'POST'])
def login_page():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']
        response = requests.post(f"{API_URL}/login", json={"username": username, "password": password})

        if response.status_code == 200:
            user_data = response.json()
            session.update({
                'token': user_data['token'],
                'user_id': user_data['user_id']
            })
            flash("Login successful!", "success")
            return redirect(url_for('dashboard', user_id=user_data['user_id']))
        else:
            flash("Invalid username or password.", "error")
    return render_template('login.html')
@app.route('/dashboard')
def dashboard():
    redirect_check = handle_redirect()
    if redirect_check:
        return redirect_check  # Gebruiker terugsturen naar login indien niet ingelogd

    headers = get_auth_headers()  # Token ophalen
    logging.debug(f"Auth Headers: {headers}")  # Debugging

    response = requests.get(f"{API_URL}/fetch", headers=headers)  # Haal nieuwste data op
    logging.debug(f"Response Status Code: {response.status_code}")  # Debugging
    logging.debug(f"Response Content: {response.content}")  # Debugging

    if response.status_code == 200:
        sensor_data = response.json()
    else:
        sensor_data = {"data_id": "-", "user_id": "-", "temperature": "-", "humidity": "-", "pressure": "-", "timestamp": "-"}
        flash(f"Fout bij ophalen van gegevens: {response.status_code}", "error")

    return render_template('home.html', sensor_data=sensor_data)

@app.route('/data', methods=["GET"])
def data_page():
    redirect_check = handle_redirect()
    if redirect_check:
        return redirect_check  # Gebruiker terugsturen naar login indien niet ingelogd

    headers = get_auth_headers()  # Haalt token uit session
    print("Verstuurde Headers:", headers)  # Debugging

    response = requests.get(f"{API_URL}/data", headers=headers)

    if response.status_code == 200:
        sensor_data = response.json()
    else:
        sensor_data = []
        flash(f"Fout bij ophalen van gegevens: {response.status_code}", "error")

    return render_template('data.html', sensor_data=sensor_data)

@app.route('/logout')
def logout():
    session.clear()
    flash("Je bent uitgelogd.", "info")
    return redirect(url_for('login_page'))

if __name__ == "__main__":
    app.run(debug=True, port=5001)
