import pygame
import numpy as np
import random
import math
from heapq import nsmallest

# Initialize Pygame
pygame.init()

# Constants
CELL_SIZE = 5
WIDTH, HEIGHT = 80, 60
SCREEN_WIDTH = WIDTH * CELL_SIZE
SCREEN_HEIGHT = HEIGHT * CELL_SIZE
INFO_HEIGHT = 80  # Increased height for info panel
WINDOW_HEIGHT = SCREEN_HEIGHT + INFO_HEIGHT
FPS = 60
FRACTAL_MAX = 100  # Maximum value for fractal layer

# Colors
BLACK = (0, 0, 0)
WHITE = (255, 255, 255)
GRAY = (128, 128, 128)
BLUE = (0, 0, 255)
GREEN = (0, 255, 0)
RED = (255, 0, 0)
YELLOW = (255, 255, 0)
CYAN = (0, 255, 255)
MAGENTA = (255, 0, 255)

# Initialize screen
screen = pygame.display.set_mode((SCREEN_WIDTH, WINDOW_HEIGHT))
pygame.display.set_caption("Conway's Game of Life with Fractal Layer")
clock = pygame.time.Clock()

# Create grid and fractal layer
grid = np.zeros((HEIGHT, WIDTH), dtype=int)
fractal_layer = np.zeros((HEIGHT, WIDTH), dtype=int)
show_fractal = False  # Option to visualize fractal layer
fractal_influence = 0.5  # Adjustable influence of fractal layer on thresholds
fractal_color_mode = 0  # Different color modes for fractal visualization
font = pygame.font.SysFont('Arial', 16)

# 3D Nearest Neighbors parameters
neighborhood_radius = 1  # Start with immediate neighbors (radius=1 means 3x3 area)
max_neighbors = 8  # Default is 8 nearest neighbors (can be adjusted)
z_weight = 0.5  # Weight of z-dimension (fractal layer) in distance calculations

# Help screen
show_help = False  # Toggle for help screen

# Initialize random grid
def initialize_random_grid(probability=0.2):
    for y in range(HEIGHT):
        for x in range(WIDTH):
            grid[y, x] = 1 if random.random() < probability else 0

# Initialize fractal layer with random values
def initialize_fractal_layer():
    global fractal_layer
    for y in range(HEIGHT):
        for x in range(WIDTH):
            fractal_layer[y, x] = random.randint(0, FRACTAL_MAX)

# Different fractal generation methods
def generate_fractal_pattern(method=0):
    """Generate a fractal pattern using various methods"""
    global fractal_layer
    
    if method == 0:  # Simple cellular automaton pattern
        # Initialize with random values
        fractal_layer = np.random.randint(0, FRACTAL_MAX // 2, (HEIGHT, WIDTH))
        
        # Apply a simple cellular automaton rule to create patterns
        for _ in range(5):  # Iterate a few times to create patterns
            new_layer = fractal_layer.copy()
            for y in range(HEIGHT):
                for x in range(WIDTH):
                    # Average of neighbors with some randomness
                    neighbors_sum = 0
                    count = 0
                    for i in range(-1, 2):
                        for j in range(-1, 2):
                            ny, nx = (y + i) % HEIGHT, (x + j) % WIDTH
                            neighbors_sum += fractal_layer[ny, nx]
                            count += 1
                    
                    avg = neighbors_sum / count
                    new_layer[y, x] = min(FRACTAL_MAX, int(avg + random.randint(-5, 5)))
            fractal_layer = new_layer
    
    elif method == 1:  # Perlin-like noise
        # Create a smoother noise pattern
        base_res = 10
        for y in range(HEIGHT):
            for x in range(WIDTH):
                # Simple approximation of Perlin noise using multiple sine waves
                value = 0
                for freq in [1, 2, 4, 8]:
                    value += math.sin(x/base_res*freq) * math.cos(y/base_res*freq) * (FRACTAL_MAX/(2*freq))
                fractal_layer[y, x] = min(FRACTAL_MAX, max(0, int(FRACTAL_MAX/2 + value)))
    
    elif method == 2:  # Circular patterns
        # Create circular wave patterns
        cx, cy = WIDTH/2, HEIGHT/2
        for y in range(HEIGHT):
            for x in range(WIDTH):
                dist = math.sqrt((x - cx)**2 + (y - cy)**2)
                value = math.sin(dist/5) * FRACTAL_MAX/2 + FRACTAL_MAX/2
                fractal_layer[y, x] = int(value)
    
    elif method == 3:  # Gradient pattern
        for y in range(HEIGHT):
            for x in range(WIDTH):
                # Create a gradient that goes from 0 to FRACTAL_MAX
                fractal_layer[y, x] = int((x / WIDTH + y / HEIGHT) / 2 * FRACTAL_MAX)

def calculate_3d_distance(x1, y1, z1, x2, y2, z2):
    """Calculate 3D Euclidean distance with z_weight for the z-dimension"""
    # Normalize x,y to same scale as z
    x_norm = (x2 - x1) / WIDTH * FRACTAL_MAX
    y_norm = (y2 - y1) / HEIGHT * FRACTAL_MAX
    
    # Calculate distance with weighted z component
    return math.sqrt(x_norm**2 + y_norm**2 + (z_weight * (z2 - z1))**2)

def get_n_nearest_neighbors(grid, x, y):
    """Get the n nearest neighbors in 3D space (x,y,fractal)"""
    center_z = fractal_layer[y, x]
    neighbors = []
    
    # Search within the neighborhood_radius
    for i in range(-neighborhood_radius, neighborhood_radius + 1):
        for j in range(-neighborhood_radius, neighborhood_radius + 1):
            if i == 0 and j == 0:  # Skip the cell itself
                continue
                
            # Use modulo for wrapping around edges (toroidal grid)
            ny, nx = (y + i) % HEIGHT, (x + j) % WIDTH
            neighbor_z = fractal_layer[ny, nx]
            
            # Calculate 3D distance
            distance = calculate_3d_distance(x, y, center_z, nx, ny, neighbor_z)
            
            # Add to neighbors list with its value and distance
            neighbors.append((distance, grid[ny, nx]))
    
    # Get the max_neighbors closest neighbors
    nearest_neighbors = nsmallest(min(max_neighbors, len(neighbors)), neighbors)
    
    # Return the sum of cell values for the nearest neighbors
    return sum(value for _, value in nearest_neighbors)

def update_grid():
    """Update the grid according to Conway's Game of Life rules with 3D nearest neighbors."""
    global grid
    new_grid = grid.copy()
    
    for y in range(HEIGHT):
        for x in range(WIDTH):
            # Get n nearest neighbors in 3D space
            neighbor_count = get_n_nearest_neighbors(grid, x, y)
            
            # Current cell's fractal value affects its survival threshold
            fractal_factor = fractal_layer[y, x] / FRACTAL_MAX
            
            # Modified rules with fractal influence
            if grid[y, x] == 1:  # Cell is alive
                # Fractal affects survival threshold
                lower_bound = 1.8 + fractal_factor * 0.4 * fractal_influence
                upper_bound = 3.2 + fractal_factor * 0.3 * fractal_influence
                
                if neighbor_count < lower_bound or neighbor_count > upper_bound:
                    new_grid[y, x] = 0  # Cell dies
            else:  # Cell is dead
                # Fractal affects reproduction threshold
                lower_bound = 2.9 - fractal_factor * 0.1 * fractal_influence
                upper_bound = 3.1 + fractal_factor * 0.3 * fractal_influence
                
                if lower_bound < neighbor_count < upper_bound:
                    new_grid[y, x] = 1  # Cell becomes alive
    
    grid = new_grid

def get_fractal_color(value):
    """Get a color for fractal visualization based on the current mode"""
    normalized = value / FRACTAL_MAX
    
    if fractal_color_mode == 0:  # Blue gradient
        intensity = int(normalized * 255)
        return (0, 0, intensity)
    
    elif fractal_color_mode == 1:  # Heat map (blue to red)
        if normalized < 0.5:
            # Blue to purple
            blue = 255
            red = int(normalized * 2 * 255)
            return (red, 0, blue)
        else:
            # Purple to red
            red = 255
            blue = int((1 - normalized) * 2 * 255)
            return (red, 0, blue)
    
    elif fractal_color_mode == 2:  # Rainbow
        if normalized < 0.2:
            return (int(normalized * 5 * 255), 0, 255)  # Blue to Purple
        elif normalized < 0.4:
            return (255, 0, int((0.4 - normalized) * 5 * 255))  # Purple to Red
        elif normalized < 0.6:
            return (255, int((normalized - 0.4) * 5 * 255), 0)  # Red to Yellow
        elif normalized < 0.8:
            return (int((0.8 - normalized) * 5 * 255), 255, 0)  # Yellow to Green
        else:
            return (0, 255, int((normalized - 0.8) * 5 * 255))  # Green to Cyan
    
    return (int(normalized * 255), int(normalized * 255), int(normalized * 255))  # Grayscale fallback

def reset_simulation():
    """Reset both grid and fractal layer to initial state"""
    global grid, fractal_layer
    initialize_random_grid()
    generate_fractal_pattern(current_fractal_method)

def draw_help_screen():
    """Draw a help screen with all available controls"""
    overlay = pygame.Surface((SCREEN_WIDTH, WINDOW_HEIGHT), pygame.SRCALPHA)
    overlay.fill((0, 0, 0, 200))  # Semi-transparent black background
    screen.blit(overlay, (0, 0))
    
    # Title
    title_font = pygame.font.SysFont('Arial', 24, bold=True)
    title_surface = title_font.render("CONTROLS", True, WHITE)
    screen.blit(title_surface, (SCREEN_WIDTH//2 - title_surface.get_width()//2, 20))
    
    # Control information
    help_font = pygame.font.SysFont('Arial', 16)
    controls = [
        "SPACE: Pause/Resume simulation",
        "ESC: Reset entire simulation",
        "R: Randomize grid only",
        "F: Regenerate fractal pattern",
        "C: Cycle color modes (Ctrl+C: Clear grid)",
        "V: Toggle fractal visualization",
        "H: Show/Hide this help screen",
        "Z/X: Increase/Decrease Z-dimension weight",
        "N/M: Increase/Decrease max neighbors",
        "+/-: Adjust fractal influence on rules",
        "SHIFT+R/T: Increase/Decrease neighborhood radius",
        "F1-F4: Different fractal patterns",
        "Left-click: Toggle cell state",
        "Right-click: Modify fractal value (when visible)"
    ]
    
    y_pos = 60
    for control in controls:
        control_surface = help_font.render(control, True, WHITE)
        screen.blit(control_surface, (SCREEN_WIDTH//2 - control_surface.get_width()//2, y_pos))
        y_pos += 30

def draw_grid():
    """Draw the grid on the screen."""
    screen.fill(BLACK)
    
    for y in range(HEIGHT):
        for x in range(WIDTH):
            if show_fractal:
                # Visualize fractal layer with color based on current mode
                color = get_fractal_color(fractal_layer[y, x])
                pygame.draw.rect(screen, color, (x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE))
                
                # Show alive cells as white dots in the middle
                if grid[y, x] == 1:
                    dot_size = CELL_SIZE // 3
                    dot_pos = (x * CELL_SIZE + CELL_SIZE//2 - dot_size//2, 
                               y * CELL_SIZE + CELL_SIZE//2 - dot_size//2)
                    pygame.draw.rect(screen, WHITE, (*dot_pos, dot_size, dot_size))
            else:
                # Standard visualization
                if grid[y, x] == 1:
                    pygame.draw.rect(screen, WHITE, (x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE))
            
            # Optionally draw grid lines
            pygame.draw.rect(screen, GRAY, (x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE), 1)
    
    # Draw information panel
    pygame.draw.rect(screen, (30, 30, 30), (0, SCREEN_HEIGHT, SCREEN_WIDTH, INFO_HEIGHT))
    
    # Display simulation state
    state_text = "RUNNING" if not paused else "PAUSED"
    state_surface = font.render(f"State: {state_text} | {'FRACTAL VIEW' if show_fractal else 'STANDARD VIEW'}", True, WHITE)
    screen.blit(state_surface, (10, SCREEN_HEIGHT + 5))
    
    # Display 3D parameters
    params_text = f"Z-Weight: {z_weight:.1f} | Radius: {neighborhood_radius} | Neighbors: {max_neighbors} | Rule Influence: {fractal_influence:.1f}"
    params_surface = font.render(params_text, True, WHITE)
    screen.blit(params_surface, (10, SCREEN_HEIGHT + 25))
    
    # Main controls row 1
    controls1 = "ESC: Reset | SPACE: Pause | R: Random Grid | F: New Fractal | V: View Mode | H: Help"
    controls1_surface = font.render(controls1, True, YELLOW)
    screen.blit(controls1_surface, (10, SCREEN_HEIGHT + 45))
    
    # Main controls row 2
    controls2 = "Z/X: Z-Weight | N/M: Neighbors | +/-: Influence | SHIFT+R/T: Radius | F1-F4: Patterns"
    controls2_surface = font.render(controls2, True, YELLOW)
    screen.blit(controls2_surface, (10, SCREEN_HEIGHT + 65))
    
    # Draw help screen if enabled
    if show_help:
        draw_help_screen()

def main():
    global show_fractal, fractal_influence, fractal_color_mode, neighborhood_radius, max_neighbors, z_weight
    global paused, current_fractal_method, show_help
    
    initialize_random_grid()
    generate_fractal_pattern(0)  # Initialize fractal layer
    
    running = True
    paused = False
    current_fractal_method = 0
    
    while running:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
            elif event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    reset_simulation()  # Complete reset
                elif event.key == pygame.K_h:
                    show_help = not show_help  # Toggle help screen
                elif event.key == pygame.K_SPACE:
                    paused = not paused
                elif event.key == pygame.K_r:
                    if pygame.key.get_mods() & pygame.KMOD_SHIFT:  # Shift+R increases radius
                        neighborhood_radius = min(5, neighborhood_radius + 1)
                    else:
                        initialize_random_grid()  # Only randomize grid
                elif event.key == pygame.K_t:
                    neighborhood_radius = max(1, neighborhood_radius - 1)  # Decrease radius
                elif event.key == pygame.K_v:
                    show_fractal = not show_fractal
                elif event.key == pygame.K_c:
                    if pygame.key.get_mods() & pygame.KMOD_CTRL:
                        grid.fill(0)  # Clear grid
                    else:
                        # Cycle through color modes
                        fractal_color_mode = (fractal_color_mode + 1) % 3
                elif event.key == pygame.K_z:
                    z_weight = min(2.0, z_weight + 0.1)  # Increase z-weight
                elif event.key == pygame.K_x:
                    z_weight = max(0.0, z_weight - 0.1)  # Decrease z-weight
                elif event.key == pygame.K_n:
                    max_neighbors = min(24, max_neighbors + 1)  # Increase max neighbors
                elif event.key == pygame.K_m:
                    max_neighbors = max(1, max_neighbors - 1)  # Decrease max neighbors
                elif event.key == pygame.K_PLUS or event.key == pygame.K_KP_PLUS or event.key == pygame.K_EQUALS:
                    fractal_influence = min(1.0, fractal_influence + 0.1)  # Increase fractal influence on rules
                elif event.key == pygame.K_MINUS or event.key == pygame.K_KP_MINUS:
                    fractal_influence = max(0.0, fractal_influence - 0.1)  # Decrease fractal influence on rules
                elif event.key == pygame.K_F1:
                    current_fractal_method = 0
                    generate_fractal_pattern(current_fractal_method)
                elif event.key == pygame.K_F2:
                    current_fractal_method = 1
                    generate_fractal_pattern(current_fractal_method)
                elif event.key == pygame.K_F3:
                    current_fractal_method = 2
                    generate_fractal_pattern(current_fractal_method)
                elif event.key == pygame.K_F4:
                    current_fractal_method = 3
                    generate_fractal_pattern(current_fractal_method)
                elif event.key == pygame.K_f:
                    generate_fractal_pattern(current_fractal_method)
            elif event.type == pygame.MOUSEBUTTONDOWN and not show_help:
                # Mouse controls (disabled when help is shown)
                x, y = event.pos
                # Left click toggles cells in grid area
                if event.button == 1 and y < SCREEN_HEIGHT:
                    grid_x, grid_y = x // CELL_SIZE, y // CELL_SIZE
                    grid[grid_y, grid_x] = 1 - grid[grid_y, grid_x]  # Toggle state
                # Right click modifies fractal layer when in fractal view
                elif event.button == 3 and y < SCREEN_HEIGHT and show_fractal:
                    grid_x, grid_y = x // CELL_SIZE, y // CELL_SIZE
                    # Increase or decrease fractal value at clicked position
                    if pygame.key.get_mods() & pygame.KMOD_SHIFT:
                        fractal_layer[grid_y, grid_x] = max(0, fractal_layer[grid_y, grid_x] - 10)
                    else:
                        fractal_layer[grid_y, grid_x] = min(FRACTAL_MAX, fractal_layer[grid_y, grid_x] + 10)
        
        if not paused and not show_help:
            update_grid()
        
        draw_grid()
        pygame.display.flip()
        clock.tick(FPS)
    
    pygame.quit()

if __name__ == "__main__":
    main()
