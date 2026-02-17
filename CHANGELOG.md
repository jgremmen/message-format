# Message Format Changelog

---
## [0.21.0](https://github.com/jgremmen/zbdd/compare/0.5.0)&nbsp;&nbsp;(2026-02-??)


### <svg xmlns="http://www.w3.org/2000/svg" style="vertical-align:top;" height="1em" viewBox="2 3 22 19"><path fill="currentColor" d="M12,21.35L10.55,20.03C5.4,15.36 2,12.27 2,8.5C2,5.41 4.42,3 7.5,3C8.17,3 8.82,3.12 9.44,3.33L13,9.35L9,14.35L12,21.35V21.35M16.5,3C19.58,3 22,5.41 22,8.5C22,12.27 18.6,15.36 13.45,20.03L12,21.35L11,14.35L15.5,9.35L12.85,4.27C13.87,3.47 15.17,3 16.5,3Z"/></svg> Breaking Changes

- Upgrade code base to java 21
- Parameter formatter notation has changed from `%{p,<format>,...}` to `%{p,format:<format>,...}` 
- Force kebab-case naming style for formatter names, template names, post formatter names and parameter config keys
- Force camel- or kebab-case naming style for parameter names
- Various code refactorings, cleanups and quality improvements (only relevant for custom formatters)


### <svg xmlns="http://www.w3.org/2000/svg" style="vertical-align:top;" height="1em" viewBox="2 2 22 19"><path fill="currentColor" d="M12,15.39L8.24,17.66L9.23,13.38L5.91,10.5L10.29,10.13L12,6.09L13.71,10.13L18.09,10.5L14.77,13.38L15.76,17.66M22,9.24L14.81,8.63L12,2L9.19,8.63L2,9.24L7.45,13.97L5.82,21L12,17.27L18.18,21L16.54,13.97L22,9.24Z"/></svg> Features

- Support message post formatting `%(clip,"message with %{param}",clip:40)`
- Auto-apply parameter formatter based on the availability of one or more configuration keys in the parameter definition
- Gradle plugin supports multiple `action { ... }` closures


### <svg xmlns="http://www.w3.org/2000/svg" style="vertical-align:top;" height="1em" viewBox="1 2 23 20"><path fill="currentColor" d="M9,2H7V4.1C6.29,4.25 5.73,4.54 5.32,4.91L2.7,2.29L1.29,3.71L4.24,6.65C4,7.39 4,8 4,8H2V10H4.04C4.1,10.63 4.21,11.36 4.4,12.15L1.68,13.05L2.31,14.95L5,14.05C5.24,14.56 5.5,15.08 5.82,15.58L3.44,17.17L4.55,18.83L7.07,17.15C7.63,17.71 8.29,18.21 9.06,18.64L8.1,20.55L9.89,21.45L10.89,19.45L10.73,19.36C11.68,19.68 12.76,19.9 14,19.97V22H16V19.93C16.76,19.84 17.81,19.64 18.77,19.19L20.29,20.71L21.7,19.29L20.37,17.95C20.75,17.44 21,16.8 21,16C21,15.5 20.95,15.08 20.88,14.68L22.45,13.9L21.55,12.1L20.18,12.79C19.63,11.96 18.91,11.5 18.29,11.28L18.95,9.32L17.05,8.68L16.29,10.96C14.96,10.83 14.17,10.32 13.7,9.77L15.45,8.9L14.55,7.1L13,7.89C12.97,7.59 12.86,6.72 12.28,5.87L13.83,3.55L12.17,2.44L10.76,4.56C10.28,4.33 9.7,4.15 9,4.06M15,18C12.06,18 9.81,17.18 8.31,15.56C5.68,12.72 6,8.2 6,8.17V8.11L6,8.03C6,7.1 6.39,6 8,6C10.63,6 10.97,7.43 11,8C11,10 12.6,13 17,13C17.33,13 19,13.15 19,16C19,17.89 15.03,18 15,18M8.5,8A1.5,1.5 0 0,0 7,9.5A1.5,1.5 0 0,0 8.5,11A1.5,1.5 0 0,0 10,9.5A1.5,1.5 0 0,0 8.5,8M11,12A1,1 0 0,0 10,13A1,1 0 0,0 11,14A1,1 0 0,0 12,13A1,1 0 0,0 11,12M15.5,14A1.5,1.5 0 0,0 14,15.5A1.5,1.5 0 0,0 15.5,17A1.5,1.5 0 0,0 17,15.5A1.5,1.5 0 0,0 15.5,14Z"/></svg> Bug Fixes

Nothing


### <svg xmlns="http://www.w3.org/2000/svg" style="vertical-align:top;" height="1em" viewBox="4 2 18 20"><path fill="currentColor" d="M6,2A2,2 0 0,0 4,4V20A2,2 0 0,0 6,22H18A2,2 0 0,0 20,20V8L14,2H6M6,4H13V9H18V20H6V4M8,12V14H16V12H8M8,16V18H13V16H8Z" /></svg> Documentation

Nothing
