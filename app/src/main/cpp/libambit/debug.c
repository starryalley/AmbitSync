/*
 * (C) Copyright 2014 Emil Ljungdahl
 *
 * This file is part of libambit.
 *
 * libambit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *
 */
#include "debug.h"

#include <stdarg.h>
#include <stdio.h>
#include <android/log.h>

#define TAG "libambit"
#define MAX_LOG_CHAR 256

/*
 * Local variables
 */
static char debug_err_text[] = "ERROR";
static char debug_warn_text[] = "WARNING";
static char debug_info_text[] = "INFO";

void debug_printf(debug_level_t level, const char *file, int line, const char *func, const char *fmt, ...)
{
    char logline[MAX_LOG_CHAR];
    va_list ap;
    enum android_LogPriority priority;

    switch (level) {
        case debug_level_err:
            priority = ANDROID_LOG_ERROR;
            break;
        case debug_level_warn:
            priority = ANDROID_LOG_WARN;
            break;
        case debug_level_info:
            priority = ANDROID_LOG_INFO;
            break;
        default:
            priority = ANDROID_LOG_DEBUG;
    }

#ifdef DEBUG_PRINT_FILE_LINE
    sprintf(logline, "%s:%d ", file, line);
#else
    // Remove compiler warning
    file = NULL;
    line = 0;
#endif
    sprintf(logline, "%s(): ", func);

    va_start(ap, fmt);
    vsprintf(logline, fmt, ap);
    va_end(ap);

    __android_log_print(priority, TAG, "%s", logline);
}
