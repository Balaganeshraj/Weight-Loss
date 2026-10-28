"""Initialize the foundation schema explicitly; future changes need versioned migrations."""
from sqlalchemy import create_engine
from .app import Base, Settings

if __name__ == "__main__":
    settings = Settings.from_env()
    settings.validate()
    engine = create_engine(settings.database_url)
    Base.metadata.create_all(engine)
    engine.dispose()
