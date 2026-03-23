import requests
import sys
import os

BASE_URL = "http://localhost:8080"
LOGIN_DATA = {
    "identifier": "0335212099",
    "password": "huy12345"
}
IMAGE_PATH = "/home/nchuy099/instagram_clone/test-image.png"

def test_upload():
    if not os.path.exists(IMAGE_PATH):
        print(f"Error: {IMAGE_PATH} not found!")
        sys.exit(1)

    print("Step 1: Logging in...")
    try:
        response = requests.post(f"{BASE_URL}/api/auth/login", json=LOGIN_DATA)
        response.raise_for_status()
        auth_data = response.json()
        token = auth_data["data"]["accessToken"]
        print("Logged in successfully!")
    except Exception as e:
        print(f"Login failed: {e}")
        if 'response' in locals():
            print(f"Response: {response.text}")
        sys.exit(1)

    print("\nStep 2: Getting presigned URL...")
    headers = {"Authorization": f"Bearer {token}"}
    params = {"fileName": "test_image.png", "contentType": "image/png"}
    try:
        response = requests.get(f"{BASE_URL}/api/media/presigned-url", headers=headers, params=params)
        response.raise_for_status()
        media_data = response.json()
        presigned_url = media_data["data"]
        print(f"Got presigned URL!")
    except Exception as e:
        print(f"Failed to get presigned URL: {e}")
        if 'response' in locals():
            print(f"Response: {response.text}")
        sys.exit(1)

    print("\nStep 3: Uploading real image file...")
    try:
        with open(IMAGE_PATH, "rb") as f:
            image_content = f.read()
        
        response = requests.put(presigned_url, data=image_content, headers={"Content-Type": "image/png"})
        response.raise_for_status()
        print(f"Upload successful! (HTTP 200). Length: {len(image_content)} bytes")
        print(f"Object uploaded to S3: {presigned_url.split('?')[0]}")
    except Exception as e:
        print(f"Upload failed: {e}")
        if 'response' in locals():
            print(f"Response status: {response.status_code}")
            print(f"Response text: {response.text}")
        sys.exit(1)

if __name__ == "__main__":
    test_upload()
