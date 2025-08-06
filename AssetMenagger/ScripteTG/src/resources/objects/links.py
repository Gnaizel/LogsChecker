from enum import Enum


class LinkType(Enum):
    DOG = "@"
    TELEGRAM = "t.me/"
    HTTPS = "https://"
    NONE = "none"
