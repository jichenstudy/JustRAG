export interface Theme {
  name: 'light' | 'dark'
  colors: {
    primary: string
    background: string
    surface: string
    text: string
    textSecondary: string
    border: string
    hover: string
    active: string
  }
}

export const lightTheme: Theme = {
  name: 'light',
  colors: {
    primary: '#161618',
    background: '#ffffff',
    surface: '#f5f5f5',
    text: '#161618',
    textSecondary: '#666666',
    border: '#e0e0e0',
    hover: '#f0f0f0',
    active: '#e8e8e8'
  }
}

export const darkTheme: Theme = {
  name: 'dark',
  colors: {
    primary: '#ffffff',
    background: '#161618',
    surface: '#1f1f21',
    text: '#ffffff',
    textSecondary: '#a0a0a0',
    border: '#2a2a2c',
    hover: '#2a2a2c',
    active: '#333335'
  }
}
