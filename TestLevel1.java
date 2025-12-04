public class TestLevel1 {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== LEVEL 1 TESTING ===");
        
        // Test 1: Start server
        System.out.println("1. Starting server...");
        // Manual verification: Server starts without errors
        
        // Test 2: Multiple client connections
        System.out.println("2. Testing multiple client connections...");
        System.out.println("   Open 3 terminals and connect clients");
        System.out.println("   Expected: All clients connect successfully");
        
        // Test 3: Message broadcasting
        System.out.println("3. Testing message broadcasting...");
        System.out.println("   Send message from Client 1");
        System.out.println("   Expected: Message appears on Client 2 & 3");
        
        // Test 4: Disconnection handling
        System.out.println("4. Testing disconnection...");
        System.out.println("   Client 2 types /quit");
        System.out.println("   Expected: Server removes client, others notified");
    }
}
