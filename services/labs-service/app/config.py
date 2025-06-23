# Import downloaded modules
from dotenv import load_dotenv

# Import built-in modules
import os

# Check if there's .env file
dotenv_path = os.path.join(os.path.dirname(__file__), '.env')
if os.path.exists(dotenv_path):
    # Load environment variables
    load_dotenv(dotenv_path)

class Config:
    # General config

    # DB config
    pass