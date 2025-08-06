import datetime

import colorama


class LoggerObject:
    def __init__(self, *, time: bool = True, debug: bool = True):
        """
        Simple logger class.
        :param time: bool - If true, will send log with log time
        :param debug: bool - If true, will send log with debug
        """
        self.__time = time
        self.__debug = debug

        # deinit colorama, if it is exists
        colorama.deinit()
        # init colorama
        colorama.init(autoreset=True)

    def log(self, message: str) -> None:
        """
        Log message to console
        :param message: str - message to log
        :return: None
        """
        if self.__time:
            print(f"{colorama.Fore.LIGHTWHITE_EX}[LOG {datetime.datetime.today().strftime('%H:%M:%S')}] {message}")
        else:
            print(f"{colorama.Fore.LIGHTWHITE_EX}[LOG] {message}")

    def info(self, message: str) -> None:
        """
        Info message to console
        :param message: str - message to log
        :return: None
        """
        if self.__time:
            print(f"{colorama.Fore.LIGHTBLUE_EX}[INFO {datetime.datetime.today().strftime('%H:%M:%S')}] {message}")
        else:
            print(f"{colorama.Fore.LIGHTBLUE_EX}[INFO] {message}")


    def warning(self, message: str) -> None:
        """
        Warning message to console
        :param message: str - message to warning
        :return: None
        """
        if self.__time:
            print(f"{colorama.Fore.YELLOW}[WARNING {datetime.datetime.today().strftime('%H:%M:%S')}] {message}")
        else:
            print(f"{colorama.Fore.YELLOW}[WARNING] {message}")

    def error(self, message: str) -> None:
        """
        Error message to console
        :param message: str - message to error
        :return: None
        """
        if self.__time:
            print(f"{colorama.Fore.LIGHTRED_EX}[ERROR {datetime.datetime.today().strftime('%H:%M:%S')}] {message}")
        else:
            print(f"{colorama.Fore.LIGHTRED_EX}[ERROR] {message}")

    def debug(self, message: str) -> None:
        """
        Debug message to console
        :param message: str - message to debug
        :return: None
        """
        if not self.__debug:
            return

        if self.__time:
            print(f"{colorama.Fore.LIGHTYELLOW_EX}[DEBUG {datetime.datetime.today().strftime('%H:%M:%S')}] {message}")
        else:
            print(f"{colorama.Fore.LIGHTYELLOW_EX}[DEBUG {datetime.datetime.today().strftime('%H:%M:%S')}] {message}")
